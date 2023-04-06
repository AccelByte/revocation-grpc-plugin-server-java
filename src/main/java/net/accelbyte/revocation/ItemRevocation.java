package net.accelbyte.revocation;

import net.accelbyte.object.RevocationStatus;
import net.accelbyte.platform.revocation.v1.RevokeItemObject;
import net.accelbyte.platform.revocation.v1.RevokeRequest;
import net.accelbyte.platform.revocation.v1.RevokeResponse;

import java.util.HashMap;
import java.util.Map;

public class ItemRevocation implements Revocation {
    @Override
    public RevokeResponse revoke(String namespace, String userId, int quantity, RevokeRequest request) {
        Map<String, String> customRevocation = new HashMap<>();

        // execute your logic, this is for demo only
        RevokeItemObject item = request.getItem();
        customRevocation.put("namespace", namespace);
        customRevocation.put("userId", userId);
        customRevocation.put("quantity", String.valueOf(quantity));
        customRevocation.put("itemId", item.getItemId());
        customRevocation.put("sku", item.getItemSku());
        customRevocation.put("itemType", item.getItemType());
        customRevocation.put("useCount", String.valueOf(item.getUseCount()));
        customRevocation.put("entitlementType", item.getEntitlementType());

        return RevokeResponse.newBuilder()
                .putAllCustomRevocation(customRevocation)
                .setStatus(RevocationStatus.SUCCESS.name()).build();
    }
}
