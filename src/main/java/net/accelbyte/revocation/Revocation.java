package net.accelbyte.revocation;

import net.accelbyte.platform.revocation.v1.RevokeRequest;
import net.accelbyte.platform.revocation.v1.RevokeResponse;

public interface Revocation {
    RevokeResponse revoke(String namespace, String userId, int quantity, RevokeRequest request);
}
