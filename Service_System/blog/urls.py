from django.urls import path, include
from . import views
from rest_framework import routers

# REST API 라우터 설정
router = routers.DefaultRouter()
router.register('Post', views.blogImage)


urlpatterns = [
    path('', views.post_list, name='post_list'),
    path('post/<int:pk>/', views.post_detail, name='post_detail'),
    path('post/new/', views.post_new, name='post_new'),
    path('post/<int:pk>/edit/', views.post_edit, name='post_edit'),
    path('js_test/', views.js_test, name='js_test'),
    path('api_root/', include(router.urls)),
    # FCM 토큰 등록 API
    path('api/fcm-token/', views.register_fcm_token, name='register_fcm_token'),
    path('api/send-notification/', views.send_order_notification, name='send_order_notification'),  # 추가!
    # 오래된 포스트 일괄 삭제 API
    path('api/delete-old-posts/', views.delete_old_posts, name='delete_old_posts'),
]