package templates

import (
	"fmt"
	"html/template"
	"net/http"
	"os"
	"path/filepath"
	"strings"

	"github.com/gin-gonic/gin"
)

var engine *template.Template

func Init(dir string) error {
	funcMap := template.FuncMap{
		"formatCurrency": func(amount float64) string {
			return fmt.Sprintf("₹%.2f", amount)
		},
		"formatDate": func(t interface{}) string {
			switch v := t.(type) {
			case nil:
				return "-"
			default:
				return fmt.Sprintf("%v", v)
			}
		},
		"eq": func(a, b interface{}) bool { return fmt.Sprintf("%v", a) == fmt.Sprintf("%v", b) },
		"gt": func(a, b int64) bool { return a > b },
		"add": func(a, b int) int { return a + b },
		"activeClass": func(activePage, page string) string {
			if activePage == page {
				return "active"
			}
			return ""
		},
		"le": func(a, b int) bool { return a <= b },
		"dict": func(values ...interface{}) (map[string]interface{}, error) {
			if len(values)%2 != 0 {
				return nil, fmt.Errorf("dict: invalid number of arguments")
			}
			m := make(map[string]interface{}, len(values)/2)
			for i := 0; i < len(values); i += 2 {
				key, ok := values[i].(string)
				if !ok {
					return nil, fmt.Errorf("dict: keys must be strings")
				}
				m[key] = values[i+1]
			}
			return m, nil
		},
	}

	pattern := filepath.Join(dir, "*.html")
	fragPattern := filepath.Join(dir, "fragments", "*.html")

	var err error
	engine, err = template.New("").Funcs(funcMap).ParseGlob(fragPattern)
	if err != nil {
		return fmt.Errorf("parse fragments: %w", err)
	}
	engine, err = engine.ParseGlob(pattern)
	if err != nil {
		return fmt.Errorf("parse templates: %w", err)
	}
	return nil
}

func Render(c *gin.Context, status int, name string, data interface{}) {
	c.Status(status)
	c.Header("Content-Type", "text/html; charset=utf-8")
	if err := engine.ExecuteTemplate(c.Writer, name, data); err != nil {
		c.String(http.StatusInternalServerError, "Template error: %v", err)
	}
}

// ConvertThymeleaf performs basic Thymeleaf-to-Go-template conversion for migration.
func ConvertThymeleaf(srcDir, dstDir string) error {
	return filepath.Walk(srcDir, func(path string, info os.FileInfo, err error) error {
		if err != nil || info.IsDir() || !strings.HasSuffix(path, ".html") {
			return err
		}
		rel, _ := filepath.Rel(srcDir, path)
		dst := filepath.Join(dstDir, rel)
		if err := os.MkdirAll(filepath.Dir(dst), 0755); err != nil {
			return err
		}
		content, err := os.ReadFile(path)
		if err != nil {
			return err
		}
		converted := convertContent(string(content))
		return os.WriteFile(dst, []byte(converted), 0644)
	})
}

func convertContent(s string) string {
	replacements := []struct{ old, new string }{
		{`xmlns:th="http://www.thymeleaf.org"`, ``},
		{`xmlns:sec="http://www.thymeleaf.org/extras/spring-security"`, ``},
		{`th:replace="~{fragments/head :: head('`, `{{template "head" (dict "Title" "`},
		{`')}"`, `")}}`},
		{`th:replace="~{fragments/layout :: sidebar}"`, `{{template "sidebar" .}}`},
		{`th:replace="~{fragments/layout :: navbar}"`, `{{template "navbar" .}}`},
		{`th:replace="~{fragments/layout :: footer}"`, `{{template "footer" .}}`},
		{`th:href="@{`, `href="`},
		{`th:src="@{`, `src="`},
		{`}"`, `"`},
		{`th:text="${`, `{{.`},
		{`}"`, `}}`},
		{`th:if="${`, `{{if .`},
		{`}"`, `}}`},
		{`th:each="`, `{{range .`},
		{` : ${`, ``},
		{`}"`, `}}`},
		{`th:classappend="${activePage == '`, `class="`},
		{`'} ? 'active' : ''"`, `"`},
		{`sec:authorize="hasRole('ADMIN')"`, `{{if .IsAdmin}}`},
		{`sec:authentication="name"`, ``},
		{`th:action="@{/logout}"`, `action="/logout"`},
		{`th:action="@{/login}"`, `action="/login"`},
		{`<input type="hidden" th:if="${_csrf != null}" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>`, ``},
	}
	for _, r := range replacements {
		s = strings.ReplaceAll(s, r.old, r.new)
	}
	// Fix common patterns
	s = strings.ReplaceAll(s, "{{.dashboard.", "{{.Dashboard.")
	s = strings.ReplaceAll(s, "{{.medicines", "{{.Medicines")
	s = strings.ReplaceAll(s, "{{.categories", "{{.Categories")
	s = strings.ReplaceAll(s, "{{.suppliers", "{{.Suppliers")
	s = strings.ReplaceAll(s, "{{.customers", "{{.Customers")
	s = strings.ReplaceAll(s, "{{.pageTitle", "{{.PageTitle")
	s = strings.ReplaceAll(s, "{{.activePage", "{{.ActivePage")
	s = strings.ReplaceAll(s, "{{.username", "{{.Username")
	s = strings.ReplaceAll(s, "{{.error", "{{.Error")
	s = strings.ReplaceAll(s, "{{.settings", "{{.Settings")
	s = strings.ReplaceAll(s, "{{.inventory", "{{.Inventory")
	s = strings.ReplaceAll(s, "{{.filter", "{{.Filter")
	s = strings.ReplaceAll(s, "{{.orders", "{{.Orders")
	s = strings.ReplaceAll(s, "{{.branches", "{{.Branches")
	s = strings.ReplaceAll(s, "{{.suggestions", "{{.Suggestions")
	s = strings.ReplaceAll(s, "{{.logs", "{{.Logs")
	s = strings.ReplaceAll(s, "{{.users", "{{.Users")
	return s
}
