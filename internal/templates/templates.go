package templates

import (
	"fmt"
	"html/template"
	"net/http"
	"path/filepath"

	"github.com/gin-gonic/gin"
)

var engine *template.Template

func Init(dir string) error {
	funcMap := template.FuncMap{
		"formatCurrency": func(amount float64) string {
			return fmt.Sprintf("₹%.2f", amount)
		},
		"formatDate": func(t interface{}) string {
			if t == nil {
				return "-"
			}
			return fmt.Sprintf("%v", t)
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
