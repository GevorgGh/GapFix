import io.agora.chat.ChatClient;
import io.agora.media.RtcTokenBuilder2;
import io.agora.chat.ChatTokenBuilder2;

public static void fetchTokenAndLogin(String uid) {
    if (ChatClient.getInstance().isLoggedIn()) return;

    String appId = "410921d5f13044e891c6ea837925553c";
    String appCert = "4c13122b3f3344a894837cb4951d8478";

    // Generate Chat user token directly on device
    String token = ChatTokenBuilder2.buildUserToken(appId, appCert, uid, 3600);

    performAgoraLogin(uid, token);
}