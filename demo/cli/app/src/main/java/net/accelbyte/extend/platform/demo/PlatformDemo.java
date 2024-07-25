/*
 * Copyright (c) 2023 AccelByte Inc. All Rights Reserved
 * This is licensed software from AccelByte Inc, for limitations
 * and restrictions contact your company contract manager.
 */
package net.accelbyte.extend.platform.demo;

import net.accelbyte.extend.platform.demo.model.SimpleItemInfo;
import net.accelbyte.sdk.api.iam.models.AccountCreateTestUserRequestV4;
import net.accelbyte.sdk.api.iam.models.AccountCreateUserResponseV4;
import net.accelbyte.sdk.api.iam.operations.users_v4.PublicCreateTestUserV4;
import net.accelbyte.sdk.api.platform.models.RevocationResult;
import net.accelbyte.sdk.api.platform.models.OrderInfo;
import net.accelbyte.sdk.api.platform.models.ItemRevocation;
import net.accelbyte.sdk.api.iam.wrappers.UsersV4;
import net.accelbyte.sdk.core.AccelByteConfig;
import net.accelbyte.sdk.core.AccelByteSDK;
import net.accelbyte.sdk.core.client.OkhttpClient;
import net.accelbyte.sdk.core.repository.DefaultConfigRepository;
import net.accelbyte.sdk.core.repository.DefaultTokenRepository;
import net.accelbyte.sdk.core.util.Helper;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

import javax.swing.text.Style;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
        name = "PlatformGrpcPluginDemo",
        description = "Revocation Grpc Plugin Demo for Platform Service",
        mixinStandardHelpOptions = true
)
public class PlatformDemo implements Callable<Integer> {

    @Mixin
    private AppConfigRepository configRepo;

    public Integer call() {
        int exitCode = 0;

        AccelByteSDK abSdk = null;
        try {
            AccelByteConfig abConfig = new AccelByteConfig(
                    new OkhttpClient(),
                    DefaultTokenRepository.getInstance(),
                    configRepo
            );

            abSdk = new AccelByteSDK(abConfig);

            System.out.println("Log in to Accelbyte...");
            System.out.println("\tBaseUrl: " + configRepo.getBaseURL());

            final boolean loginResult = abSdk.loginClient();
            if (!loginResult) {
                throw new Exception("Login failed!");
            }
            System.out.println("Login success!");

            String nameId = Helper.generateUUID();
            String username = "extend_" + nameId + "_user";
            String password = "extend(0)";
            String displayName = "Extend Test User " + nameId;
            String emailAddress = username + "@dummy.net";

            UsersV4 userWrapper = new UsersV4(abSdk);
            AccountCreateUserResponseV4 userInfo =
                userWrapper.publicCreateTestUserV4(
                    PublicCreateTestUserV4.builder()
                    .namespace(configRepo.getNamespace())
                    .body(AccountCreateTestUserRequestV4.builder()
                        .authType("EMAILPASSWD")
                        .country("ID")
                        .dateOfBirth("1990-01-01")
                        .emailAddress(emailAddress)
                        .password(password)
                        .passwordMD5Sum("")
                        .username(username)
                        .verified(true)
                        .uniqueDisplayName(displayName)
                        .build())
                    .build());
            if (userInfo == null)
                throw new Exception("Could not retrieve login user info.");
            System.out.println("User: " + userInfo.getUsername());

            final String categoryPath = configRepo.getCategoryPath();

            PlatformDataUnit pdu = new PlatformDataUnit(abSdk, configRepo);
            try {
                System.out.print("Configuring platform service grpc target... ");
                pdu.setPlatformServiceGrpcTargetForRevocation();
                System.out.println("[OK]");

                System.out.print("Creating store... ");
                pdu.createStore(true);
                System.out.println("[OK]");

                System.out.print("Creating category... ");
                pdu.createCategory(categoryPath,true);
                System.out.println("[OK]");

                System.out.print("Updating Revocation config...");
                pdu.updateRevocationConfig();
                System.out.println("[OK]");

                System.out.print("Setting up virtual currency [VCA]...");
                pdu.createCurrency();
                System.out.println("[OK]");

                System.out.print("Creating items...");
                List<SimpleItemInfo> itemInfos = pdu.createItems(1, categoryPath, true);
                System.out.println("[OK]");

                System.out.print("Creating order...");
                OrderInfo orderInfo = pdu.createOrder(userInfo.getUserId(), itemInfos);
                System.out.println("[OK]");

                System.out.print("Revoking order...");
                RevocationResult revocationResult = pdu.revoke(userInfo.getUserId(), orderInfo.getOrderNo(), orderInfo.getItemId());
                System.out.println("[OK]");

                System.out.println("Revocation Result: ");
                System.out.println("revocation history Id: " + revocationResult.getId());
                System.out.println("revocation status: " + revocationResult.getStatus());

                for (ItemRevocation revocation : revocationResult.getItemRevocations()) {
                    System.out.println("item Id: " + revocation.getItemId());
                    System.out.println("item sku: " + revocation.getItemSku());
                    System.out.println("item type: " + revocation.getItemType());
                    System.out.println("quantity: " + revocation.getQuantity());
                    System.out.println("revocation strategy: " + revocation.getStrategy());
                    System.out.println("skipped: " + revocation.getSkipped());
                    System.out.println("reason: " + revocation.getReason());
                    System.out.println("custom revocation: " + revocation.getCustomRevocation());
                }
            } catch (Exception ix) {
                System.out.println("[FAILED] " + ix.getMessage());
            } finally {
                System.out.print("Deleting currency[VCA]... ");
                pdu.deleteCurrency();
                System.out.println("[OK]");

                System.out.print("Deleting store... ");
                pdu.deleteStore();
                System.out.println("[OK]");

                pdu.unsetPlatformServiceGrpcTargetForRevocation();
            }
        } catch (Exception x) {
            System.out.println("There are some error(s). " + x.getMessage());
            exitCode = 1;
        } finally {
            if (abSdk != null)
                abSdk.logout();
        }

        return exitCode;
    }
}
