from blog.models import Post
from rest_framework import serializers
from django.contrib.auth.models import User

class PostSerializer(serializers.HyperlinkedModelSerializer):
    author = serializers.PrimaryKeyRelatedField(queryset=User.objects.all())
    id = serializers.IntegerField(read_only=True)  # id 필드 추가
    
    class Meta:
        model = Post
        fields = ('id', 'author', 'title', 'text', 'created_date', 'published_date', 'image', 'customer_type')
    
    def create(self, validated_data):
        # customer_type에 따라 title 자동 설정
        customer_type = validated_data.get('customer_type', 'new')
        
        if customer_type == 'call':
            validated_data['title'] = '손님 호출'
        elif customer_type == 'delivery':
            validated_data['title'] = '배달원'
        elif customer_type == 'new':
            # title이 없거나 기본값인 경우에만 설정
            if not validated_data.get('title') or validated_data.get('title') == '':
                validated_data['title'] = '새로운 손님'
        
        return super().create(validated_data)
    
    def update(self, instance, validated_data):
        # customer_type에 따라 title 자동 설정
        customer_type = validated_data.get('customer_type', instance.customer_type)
        
        if customer_type == 'call':
            validated_data['title'] = '손님 호출'
        elif customer_type == 'delivery':
            validated_data['title'] = '배달원'
        elif customer_type == 'new':
            # title이 없거나 기본값인 경우에만 설정
            if 'title' not in validated_data or not validated_data.get('title') or validated_data.get('title') == '':
                validated_data['title'] = '손님'
        
        return super().update(instance, validated_data)