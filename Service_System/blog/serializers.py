from blog.models import Post
from rest_framework import serializers
from django.contrib.auth.models import User

class PostSerializer(serializers.HyperlinkedModelSerializer):
    author = serializers.PrimaryKeyRelatedField(queryset=User.objects.all())
    id = serializers.IntegerField(read_only=True)  # id 필드 추가
    
    class Meta:
        model = Post
        fields = ('id', 'author', 'title', 'text', 'created_date', 'published_date', 'image', 'customer_type')