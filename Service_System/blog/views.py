from django.shortcuts import render, get_object_or_404, redirect
from django.utils import timezone
from django.db.models import Q
from .models import Post
from rest_framework import viewsets
from .serializers import PostSerializer
from .fcm_utils import send_fcm_to_multiple

# 기존 post_list 함수 (그대로 유지)
def post_list(request):
    # published_date가 None이거나 현재 시간 이전인 포스트를 모두 표시
    posts = Post.objects.filter(
        Q(published_date__isnull=True) | Q(published_date__lte=timezone.now())
    ).order_by('-published_date', '-created_date')
    return render(request, 'blog/post_list.html', {'posts': posts})

# 기존 blogImage 클래스 (그대로 유지)
class blogImage(viewsets.ModelViewSet):
    queryset = Post.objects.all()
    serializer_class = PostSerializer

# 아래 함수들 추가
def post_detail(request, pk):
    post = get_object_or_404(Post, pk=pk)
    return render(request, 'blog/post_detail.html', {'post': post})

def post_new(request):
    if request.method == "POST":
        # 폼 처리 로직
        # 여기서는 간단하게 구현
        from .forms import PostForm
        form = PostForm(request.POST, request.FILES)
        if form.is_valid():
            post = form.save(commit=False)
            post.author = request.user
            post.published_date = timezone.now()
            post.save()
            return redirect('post_detail', pk=post.pk)
    else:
        from .forms import PostForm
        form = PostForm()
    return render(request, 'blog/post_edit.html', {'form': form})

def post_edit(request, pk):
    post = get_object_or_404(Post, pk=pk)
    if request.method == "POST":
        from .forms import PostForm
        form = PostForm(request.POST, request.FILES, instance=post)
        if form.is_valid():
            post = form.save(commit=False)
            post.author = request.user
            post.published_date = timezone.now()
            post.save()
            return redirect('post_detail', pk=post.pk)
    else:
        from .forms import PostForm
        form = PostForm(instance=post)
    return render(request, 'blog/post_edit.html', {'form': form})

def js_test(request):
    return render(request, 'blog/js_test.html')



from rest_framework.decorators import api_view
from rest_framework.response import Response
from rest_framework import status
from .models import FCMToken
from datetime import date

@api_view(['POST'])
def register_fcm_token(request):
    """
    FCM 토큰 등록 API
    POST /api/fcm-token/
    Body: {"token": "fcm_device_token"}
    """
    token = request.data.get('token')
    
    if not token:
        return Response(
            {'error': '토큰이 필요합니다.'},
            status=status.HTTP_400_BAD_REQUEST
        )
    
    # 토큰 저장 또는 업데이트
    fcm_token, created = FCMToken.objects.update_or_create(
        token=token,
        defaults={'is_active': True}
    )
    
    if created:
        return Response(
            {'message': '토큰이 등록되었습니다.'},
            status=status.HTTP_201_CREATED
        )
    else:
        return Response(
            {'message': '토큰이 업데이트되었습니다.'},
            status=status.HTTP_200_OK
        )


from .fcm_utils import send_fcm_to_multiple

@api_view(['POST'])
def send_order_notification(request):
    """
    새 주문(사람 인식) 알림 전송 API
    POST /api/send-notification/
    Body: {"title": "person", "text": "새로운 손님이 감지되었습니다."}
    """
    title = request.data.get('title', '새 주문')
    text = request.data.get('text', '손님이 오셨습니다')
    customer_type = request.data.get('customer_type', 'new')  # 추가!
    

    # customer_type에 따라 알림 메시지 변경
    if customer_type == 'call':
        notification_title = "🔔 손님 호출!"
        notification_body = "손님이 호출하셨습니다!"
    elif customer_type == 'delivery':
        notification_title = "📦 배달원 입장"
        notification_body = "배달원이 도착했습니다!"
    else:  # 'new'
        notification_title = f"⭐ {title} 감지!"
        notification_body = f"새로운 손님이 입장하셨습니다! {text}"


    # 활성화된 모든 FCM 토큰 가져오기
    active_tokens = FCMToken.objects.filter(is_active=True).values_list('token', flat=True)
    
    if not active_tokens:
        return Response(
            {'message': '등록된 디바이스가 없습니다.'},
            status=status.HTTP_404_NOT_FOUND
        )
    
    # FCM 전송
    result = send_fcm_to_multiple(
        tokens=list(active_tokens),
        title=notification_title,
        body=notification_body,
        data={'type': customer_type, 'title': title}
    )
    
    return Response({
        'message': 'FCM 전송 완료',
        'success_count': result['success_count'],
        'failure_count': result['failure_count'],
    }, status=status.HTTP_200_OK)


@api_view(['DELETE'])
def delete_old_posts(request):
    """
    오늘 날짜가 아닌 Post들을 일괄 삭제
    DELETE /api/delete-old-posts/
    """
    today = date.today()
    
    # created_date가 오늘이 아닌 Post들 필터링
    old_posts = Post.objects.exclude(
        created_date__date=today
    )
    
    count = old_posts.count()
    old_posts.delete()
    
    return Response({
        'message': f'{count}개의 오래된 포스트가 삭제되었습니다.',
        'deleted_count': count
    }, status=status.HTTP_200_OK)