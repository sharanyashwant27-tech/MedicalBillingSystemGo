package middleware

import (
	"net/http"
	"strings"

	"github.com/gin-contrib/sessions"
	"github.com/gin-gonic/gin"
	"github.com/medicalbilling/medical-billing-system/internal/auth"
	"github.com/medicalbilling/medical-billing-system/internal/models"
	"github.com/medicalbilling/medical-billing-system/internal/services"
)

const SessionUserKey = "user_id"
const SessionUsernameKey = "username"

type AuthMiddleware struct {
	JWT      *auth.JWTManager
	Services *services.Services
}

func NewAuthMiddleware(jwt *auth.JWTManager, svc *services.Services) *AuthMiddleware {
	return &AuthMiddleware{JWT: jwt, Services: svc}
}

func BlockQuerySecrets() gin.HandlerFunc {
	blocked := []string{"token", "jwt", "access_token", "api_key", "apikey", "password", "secret"}
	return func(c *gin.Context) {
		for _, key := range blocked {
			if c.Query(key) != "" {
				c.AbortWithStatusJSON(http.StatusBadRequest, gin.H{"message": "Security tokens must not be sent in the URL."})
				return
			}
		}
		c.Next()
	}
}

func (m *AuthMiddleware) Authenticate() gin.HandlerFunc {
	return func(c *gin.Context) {
		if user := m.userFromSession(c); user != nil {
			c.Set("user", user)
			c.Set("username", user.Username)
			c.Next()
			return
		}

		authHeader := c.GetHeader("Authorization")
		if authHeader != "" && strings.HasPrefix(authHeader, "Bearer ") {
			token := strings.TrimPrefix(authHeader, "Bearer ")
			claims, err := m.JWT.ParseToken(token)
			if err == nil {
				user, err := m.Services.GetUserByID(claims.UserID)
				if err == nil && user.Enabled && user.AccountNonLocked {
					c.Set("user", user)
					c.Set("username", user.Username)
					c.Set("claims", claims)
					c.Next()
					return
				}
			}
		}

		if strings.HasPrefix(c.Request.URL.Path, "/api/") {
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{"message": "Unauthorized"})
			return
		}
		c.Redirect(http.StatusFound, "/login")
		c.Abort()
	}
}

func (m *AuthMiddleware) RequireRole(roles ...models.RoleType) gin.HandlerFunc {
	return func(c *gin.Context) {
		userVal, exists := c.Get("user")
		if !exists {
			c.AbortWithStatusJSON(http.StatusUnauthorized, gin.H{"message": "Unauthorized"})
			return
		}
		user := userVal.(*models.User)
		for _, role := range roles {
			if user.HasRole(role) {
				c.Next()
				return
			}
		}
		c.AbortWithStatusJSON(http.StatusForbidden, gin.H{"message": "Forbidden"})
	}
}

func (m *AuthMiddleware) userFromSession(c *gin.Context) *models.User {
	session := sessions.Default(c)
	userID, ok := session.Get(SessionUserKey).(uint)
	if !ok || userID == 0 {
		if f, ok := session.Get(SessionUserKey).(float64); ok {
			userID = uint(f)
		} else {
			return nil
		}
	}
	user, err := m.Services.GetUserByID(userID)
	if err != nil {
		return nil
	}
	return user
}

func SetSessionUser(c *gin.Context, user *models.User) {
	session := sessions.Default(c)
	session.Set(SessionUserKey, user.ID)
	session.Set(SessionUsernameKey, user.Username)
	_ = session.Save()
}

func ClearSession(c *gin.Context) {
	session := sessions.Default(c)
	session.Clear()
	_ = session.Save()
}
