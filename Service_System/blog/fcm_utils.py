from firebase_admin import messaging
import logging

logger = logging.getLogger(__name__)


def send_fcm_notification(token, title, body, data=None):
    """
    FCM 푸시 알림 전송 (단일 디바이스)
    """
    try:
        message = messaging.Message(
            notification=messaging.Notification(
                title=title,
                body=body,
            ),
            data=data or {},
            token=token,
        )
        
        response = messaging.send(message)
        logger.info(f'FCM 전송 성공: {response}')
        return True
        
    except Exception as e:
        logger.error(f'FCM 전송 실패: {str(e)}')
        return False


def send_fcm_to_multiple(tokens, title, body, data=None):
    """
    여러 디바이스에 FCM 푸시 알림 전송 (반복문 사용)
    """
    success_count = 0
    failure_count = 0
    
    for token in tokens:
        try:
            message = messaging.Message(
                notification=messaging.Notification(
                    title=title,
                    body=body,
                ),
                data=data or {},
                token=token,
            )
            
            response = messaging.send(message)
            logger.info(f'FCM 전송 성공: {response}')
            success_count += 1
            
        except Exception as e:
            logger.error(f'FCM 전송 실패 (토큰: {token[:20]}...): {str(e)}')
            failure_count += 1
    
    logger.info(f'{success_count}개 전송 성공, {failure_count}개 전송 실패')
    
    return {
        'success_count': success_count,
        'failure_count': failure_count,
    }