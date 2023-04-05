package net.accelbyte.revocation;

import net.accelbyte.object.RevocationStatus;
import net.accelbyte.platform.revocation.v1.RevokeCurrencyObject;
import net.accelbyte.platform.revocation.v1.RevokeRequest;
import net.accelbyte.platform.revocation.v1.RevokeResponse;

import java.util.HashMap;
import java.util.Map;

public class CurrencyRevocation implements Revocation {
    @Override
    public RevokeResponse revoke(String namespace, String userId, int quantity, RevokeRequest request) {
        Map<String, String> customRevocation = new HashMap<>();

        // execute your logic, this is for demo only
        RevokeCurrencyObject currency = request.getCurrency();
        customRevocation.put("namespace", namespace);
        customRevocation.put("userId", userId);
        customRevocation.put("quantity", String.valueOf(quantity));
        customRevocation.put("currencyNamespace", currency.getNamespace());
        customRevocation.put("currencyCode", currency.getCurrencyCode());
        customRevocation.put("balanceOrigin", currency.getBalanceOrigin());

        return RevokeResponse.newBuilder()
                .putAllCustomRevocation(customRevocation)
                .setStatus(RevocationStatus.SUCCESS.name()).build();
    }
}
