package controllers

import (
	"net/http"
	"github.com/gin-gonic/gin"
)

type DefaultController struct {}

func NewDefaultController() *DefaultController {
    return &DefaultController{}
}

func (c *DefaultController) Home(ctx *gin.Context) {
    ctx.JSON(http.StatusOK, gin.H{
        "message": "Welcome to the private home endpoint!",
        "status":  "success",
    })
}