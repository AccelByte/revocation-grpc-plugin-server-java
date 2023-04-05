package net.accelbyte.revocation;

import net.accelbyte.object.RevokeEntryType;

import java.util.Map;

public class Revocations {
    private static final Map<RevokeEntryType, Revocation> REVOCATIONS = Map.ofEntries(
            Map.entry(RevokeEntryType.ITEM, new ItemRevocation()),
            Map.entry(RevokeEntryType.CURRENCY, new CurrencyRevocation()),
            Map.entry(RevokeEntryType.ENTITLEMENT, new EntitlementRevocation())
    );

    public static Revocation getRevocation(RevokeEntryType type) {
        Revocation revocation = REVOCATIONS.get(type);

        if (revocation == null) {
            throw new Error(String.format("Revocation method %s not supported", type.toString()));
        }

        return revocation;
    }
}
