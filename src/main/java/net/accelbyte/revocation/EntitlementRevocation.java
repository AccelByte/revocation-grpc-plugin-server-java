package net.accelbyte.revocation;

import net.accelbyte.object.RevocationStatus;
import net.accelbyte.platform.revocation.v1.RevokeEntitlementObject;
import net.accelbyte.platform.revocation.v1.RevokeRequest;
import net.accelbyte.platform.revocation.v1.RevokeResponse;

import java.util.HashMap;
import java.util.Map;

public class EntitlementRevocation implements Revocation {
    @Override
    public RevokeResponse revoke(String namespace, String userId, int quantity, RevokeRequest request) {
        Map<String, String> customRevocation = new HashMap<>();

        // execute your logic, this is for demo only
        RevokeEntitlementObject entitlement = request.getEntitlement();
        customRevocation.put("namespace", namespace);
        customRevocation.put("userId", userId);
        customRevocation.put("quantity", String.valueOf(quantity));
        customRevocation.put("entitlementId", entitlement.getEntitlementId());
        customRevocation.put("itemId", entitlement.getItemId());
        customRevocation.put("sku", entitlement.getSku());

        return RevokeResponse.newBuilder()
                .putAllCustomRevocation(customRevocation)
                .setStatus(RevocationStatus.SUCCESS.name()).build();
    }
}
