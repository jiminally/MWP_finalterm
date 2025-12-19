from django.contrib import admin
from .models import Post, FCMToken

admin.site.register(Post)  # 관리자 페이지에서 'Post' 모델 확인

# FCMToken 모델 추가
@admin.register(FCMToken)
class FCMTokenAdmin(admin.ModelAdmin):
    list_display = ['token', 'device_type', 'is_active', 'created_at']
    list_filter = ['is_active', 'device_type']
    search_fields = ['token']