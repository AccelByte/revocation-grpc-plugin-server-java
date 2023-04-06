package net.accelbyte.service;


import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;


import net.accelbyte.object.RevocationStatus;
import net.accelbyte.object.RevokeEntryType;
import net.accelbyte.platform.revocation.v1.*;
import net.accelbyte.revocation.Revocation;
import net.accelbyte.revocation.Revocations;
import org.lognet.springboot.grpc.GRpcService;

@Slf4j
@GRpcService
public class RevocationServiceImplementation extends RevocationGrpc.RevocationImplBase {
    @Override
    public void revoke(RevokeRequest request, StreamObserver<RevokeResponse> responseObserver) {
        log.info("Received revoke request");

        RevokeResponse response;

        try {
            String namespace = request.getNamespace();
            String userId = request.getUserId();
            int quantity = request.getQuantity();

            RevokeEntryType revokeEntryType = RevokeEntryType.valueOf(request.getRevokeEntryType().toUpperCase());
            Revocation revocation = Revocations.getRevocation(revokeEntryType);
            response = revocation.revoke(namespace, userId, quantity, request);
        } catch (Throwable ex) {
            response = RevokeResponse.newBuilder()
                    .setStatus(RevocationStatus.FAIL.name())
                    .setReason(ex.getMessage())
                    .build();
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
