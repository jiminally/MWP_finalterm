from django.conf import settings
from django.db import models
from django.utils import timezone


class Post(models.Model):
    # 새로 추가! 👇
    CUSTOMER_TYPE_CHOICES = [
        ('new', '새로운 손님'),
        ('call', '손님 호출'),
        ('delivery', '배달원'),
    ]
    
    author = models.ForeignKey(settings.AUTH_USER_MODEL, on_delete=models.CASCADE)
    title = models.CharField(max_length=200)
    text = models.TextField()
    created_date = models.DateTimeField(default=timezone.now)
    published_date = models.DateTimeField(blank=True, null=True)
    image = models.ImageField(upload_to='blog_image/%Y/%m/%d/', default='blog_image/default_error.png')
    
    # 새로 추가! 👇
    customer_type = models.CharField(
        max_length=10,
        choices=CUSTOMER_TYPE_CHOICES,
        default='new'
    )

    def publish(self):
        self.published_date = timezone.now()
        self.save()

    def __str__(self):
        return self.title


class FCMToken(models.Model):
    token = models.CharField(max_length=255, unique=True)
    device_type = models.CharField(max_length=20, default='android')
    created_at = models.DateTimeField(auto_now_add=True)
    updated_at = models.DateTimeField(auto_now=True)
    is_active = models.BooleanField(default=True)
    
    def __str__(self):
        return f"{self.device_type} - {self.token[:20]}..."
    
    class Meta:
        db_table = 'fcm_tokens'
        verbose_name = 'FCM 토큰'
        verbose_name_plural = 'FCM 토큰들'