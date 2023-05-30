/*
 * Copyright (c) 2023 AccelByte Inc. All Rights Reserved
 * This is licensed software from AccelByte Inc, for limitations
 * and restrictions contact your company contract manager.
 */
package net.accelbyte.extend.platform.demo;

import net.accelbyte.extend.platform.demo.model.SimpleItemInfo;
import net.accelbyte.sdk.api.platform.models.*;
import net.accelbyte.sdk.api.platform.operations.revocation.*;
import net.accelbyte.sdk.api.platform.operations.currency.*;
import net.accelbyte.sdk.api.platform.operations.wallet.*;
import net.accelbyte.sdk.api.platform.operations.order.*;
import net.accelbyte.sdk.api.platform.operations.catalog_changes.PublishAll;
import net.accelbyte.sdk.api.platform.operations.category.CreateCategory;
import net.accelbyte.sdk.api.platform.operations.item.CreateItem;
import net.accelbyte.sdk.api.platform.operations.section.CreateSection;
import net.accelbyte.sdk.api.platform.operations.section.PublicListActiveSections;
import net.accelbyte.sdk.api.platform.operations.section.UpdateSection;
import net.accelbyte.sdk.api.platform.operations.service_plugin_config.DeleteServicePluginConfig;
import net.accelbyte.sdk.api.platform.operations.service_plugin_config.UpdateServicePluginConfig;
import net.accelbyte.sdk.api.platform.operations.store.CreateStore;
import net.accelbyte.sdk.api.platform.operations.store.DeleteStore;
import net.accelbyte.sdk.api.platform.operations.store.ListStores;
import net.accelbyte.sdk.api.platform.operations.view.CreateView;
import net.accelbyte.sdk.api.platform.wrappers.*;
import net.accelbyte.sdk.core.AccelByteSDK;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class PlatformDataUnit {
    private final AccelByteSDK abSdk;

    private final AppConfigRepository config;

    private final String abStoreName = "Revocation Plugin Demo Store";

    private final String abStoreDesc = "Description for revocation grpc plugin demo store";

    private final String abViewName = "Revocation Default View";

    private String currencyCode = "VCA";

    private final String abNamespace;

    private String storeId = "";

    private String viewId = "";


    public PlatformDataUnit(AccelByteSDK sdk, AppConfigRepository configRepo) throws Exception {
        abSdk = sdk;
        config = configRepo;
        abNamespace = configRepo.getNamespace();
    }

    protected String getRandomString(String characters, int length) {
        final Random random = new Random();
        final char[] result = new char[length];
        for (int i = 0; i < result.length; i++) {
            while (true) {
                result[i] = characters.charAt(random.nextInt(characters.length()));
                if (i > 0 && result[i - 1] == result[i])
                    continue;
                else break;
            }
        }
        return new String(result);
    }

    public void publishStoreChange() throws Exception {
        try {
            final PublishAll publishAllOp = PublishAll.builder()
                    .namespace(abNamespace)
                    .storeId(storeId)
                    .build();
            CatalogChanges wrapper = new CatalogChanges(abSdk);
            wrapper.publishAll(publishAllOp);
        } catch (Exception x) {
            System.out.println("Could not publish store changes. " + x.getMessage());
            throw x;
        }
    }

    public RevocationConfigInfo updateRevocationConfig() throws Exception {
        final Revocation revocationWrapper = new Revocation(abSdk);

        final WalletRevocationConfig walletConfig = WalletRevocationConfig.builder()
                .enabled(Boolean.TRUE)
                .strategy(WalletRevocationConfig.Strategy.CUSTOM.name())
                .build();

        final DurableEntitlementRevocationConfig durableEntitlementRevocationConfig = DurableEntitlementRevocationConfig.builder()
                .enabled(Boolean.FALSE)
                .strategy(DurableEntitlementRevocationConfig.Strategy.CUSTOM.name())
                .build();

        final EntitlementRevocationConfig entitlementRevocationConfig = EntitlementRevocationConfig.builder()
                .durable(durableEntitlementRevocationConfig)
                .build();

        final RevocationConfigUpdate revocationConfigUpdate = RevocationConfigUpdate.builder()
                .entitlement(entitlementRevocationConfig)
                .wallet(walletConfig)
                .build();

        final UpdateRevocationConfig updateRevocationConfig = UpdateRevocationConfig.builder()
                .namespace(abNamespace)
                .body(revocationConfigUpdate)
                .build();

        return revocationWrapper.updateRevocationConfig(updateRevocationConfig);
    }

    public CurrencyInfo createCurrency() throws Exception {
        final net.accelbyte.sdk.api.platform.wrappers.Currency currencyWrapper = new net.accelbyte.sdk.api.platform.wrappers.Currency(abSdk);

        final CurrencyCreate currencyCreate = CurrencyCreate.builder()
                .currencyCode(currencyCode)
                .currencySymbol("$V")
                .currencyType(CurrencyCreate.CurrencyType.VIRTUAL.name())
                .decimals(0)
                .build();

        final CreateCurrency createCurrency = CreateCurrency.builder()
                .namespace(abNamespace)
                .body(currencyCreate)
                .build();

        return currencyWrapper.createCurrency(createCurrency);
    }

    public CurrencyInfo deleteCurrency() throws Exception {
        final net.accelbyte.sdk.api.platform.wrappers.Currency currencyWrapper = new net.accelbyte.sdk.api.platform.wrappers.Currency(abSdk);
        final DeleteCurrency deleteCurrency = DeleteCurrency.builder()
                .namespace(abNamespace)
                .currencyCode(currencyCode)
                .build();

        return currencyWrapper.deleteCurrency(deleteCurrency);
    }


    public String createStore(boolean doPublish) throws Exception {

        final ListStores listStoresOp = ListStores.builder()
                .namespace(abNamespace)
                .build();

        Store storeWrapper = new Store(abSdk);
        final List<StoreInfo> stores = storeWrapper.listStores(listStoresOp);
        if ((stores != null) && (stores.size() > 0)) {
            //clean up draft stores
            for (StoreInfo store : stores) {
                if (!store.getPublished()) {
                    storeWrapper.deleteStore(DeleteStore.builder()
                            .namespace(abNamespace)
                            .storeId(store.getStoreId())
                            .build());
                }
            }
        }

        final List<String> sLangs = new ArrayList<>();
        sLangs.add("en");

        final List<String> sRegions = new ArrayList<>();
        sRegions.add("US");

        final StoreInfo newStore = storeWrapper.createStore(CreateStore.builder()
                .namespace(abNamespace)
                .body(StoreCreate.builder()
                        .title(abStoreName)
                        .description(abStoreDesc)
                        .defaultLanguage("en")
                        .defaultRegion("US")
                        .supportedLanguages(sLangs)
                        .supportedRegions(sRegions)
                        .build())
                .build());
        if (newStore == null)
            throw new Exception("Could not create new store.");
        storeId = newStore.getStoreId();

        if (doPublish)
            publishStoreChange();

        return storeId;
    }

    public void createCategory(String categoryPath, boolean doPublish) throws Exception {
        if (storeId.equals(""))
            throw new Exception("No store id stored.");

        Map<String,String> localz = new HashMap<>();
        localz.put("en",categoryPath);

        Category categoryWrapper = new Category(abSdk);
        categoryWrapper.createCategory(CreateCategory.builder()
                .namespace(abNamespace)
                .storeId(storeId)
                .body(CategoryCreate.builder()
                        .categoryPath(categoryPath)
                        .localizationDisplayNames(localz)
                        .build())
                .build());
    }

    public List<SimpleItemInfo> createItems(int itemCount, String categoryPath, boolean doPublish) throws Exception {
        if (storeId.equals(""))
            throw new Exception("No store id stored.");

        final String itemDiff = getRandomString("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789",6);
        Item itemWrapper = new Item(abSdk);
        List<SimpleItemInfo> nItems = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {

            SimpleItemInfo nItemInfo = new SimpleItemInfo();
            nItemInfo.setTitle("Item " + itemDiff + " Titled " + Integer.toString(i + 1));
            nItemInfo.setSku("SKU_" + itemDiff + "_" + Integer.toString(i + 1));

            final Map<String, Localization> iLocalization = new HashMap<>();
            iLocalization.put("en",Localization.builder()
                    .title(nItemInfo.getTitle())
                    .build());

            final Map<String,List<RegionDataItemDTO>> iRegionData = new HashMap<>();
            final List<RegionDataItemDTO> regionItem = new ArrayList<>();
            regionItem.add(RegionDataItemDTO.builder()
                    .currencyCode(currencyCode)
                    .currencyNamespace(abNamespace)
                    .currencyTypeFromEnum(RegionDataItemDTO.CurrencyType.VIRTUAL)
                    .price(0)
                    .build());
            iRegionData.put("US",regionItem);

            final FullItemInfo newItem = itemWrapper.createItem(CreateItem.builder()
                    .namespace(abNamespace)
                    .storeId(storeId)
                    .body(ItemCreate.builder()
                            .name(nItemInfo.getTitle())
                            .itemTypeFromEnum(ItemCreate.ItemType.COINS)
                            .categoryPath(categoryPath)
                            .entitlementTypeFromEnum(ItemCreate.EntitlementType.CONSUMABLE)
                            .seasonTypeFromEnum(ItemCreate.SeasonType.TIER)
                            .statusFromEnum(ItemCreate.Status.ACTIVE)
                            .useCount(1)
                            .targetCurrencyCode(currencyCode)
                            .listable(true)
                            .purchasable(true)
                            .sku(nItemInfo.getSku())
                            .localizations(iLocalization)
                            .regionData(iRegionData)
                            .build())
                    .build());

            if (newItem == null)
                throw new Exception("Could not create store item");

            nItemInfo.setId(newItem.getItemId());
            nItems.add(nItemInfo);
        }

        if (doPublish)
            publishStoreChange();
        return nItems;
    }

    public OrderInfo createOrder(String userId, List<SimpleItemInfo> itemInfoList) throws Exception {
        SimpleItemInfo simpleItemInfo = itemInfoList.get(0);

        OrderCreate orderCreate = OrderCreate.builder()
                .currencyCode(currencyCode)
                .itemId(simpleItemInfo.getId())
                .price(0)
                .quantity(1)
                .discountedPrice(0)
                .build();

        PublicCreateUserOrder publicCreateUserOrder = PublicCreateUserOrder.builder()
                .namespace(abNamespace)
                .userId(userId)
                .body(orderCreate)
                .build();

        net.accelbyte.sdk.api.platform.wrappers.Order orderWrapper = new net.accelbyte.sdk.api.platform.wrappers.Order(abSdk);
        return orderWrapper.publicCreateUserOrder(publicCreateUserOrder);
    }

    public RevocationResult revoke(String userId, String orderNum, String itemId) throws Exception {
        RevokeItem item = RevokeItem.builder()
                .itemIdentityType(RevokeItem.ItemIdentityType.ITEMID.toString())
                .itemIdentity(itemId)
                .build();

        RevokeEntry entry = RevokeEntry.builder()
                .item(item)
                .quantity(1)
                .type(RevokeEntry.Type.ITEM.name()).build();

        RevocationRequest request = RevocationRequest.builder()
                .source(RevocationRequest.Source.ORDER.name())
                .transactionId(orderNum)
                .revokeEntries(List.of(entry)).build();
        DoRevocation input = DoRevocation.builder().namespace(abNamespace).userId(userId).body(request).build();
        Revocation revocationWrapper = new Revocation(abSdk);
        return revocationWrapper.doRevocation(input);
    }

    public void setPlatformServiceGrpcTarget() throws Exception {
        final String abGrpcServerUrl = config.getGrpcServerUrl();
        if (abGrpcServerUrl.equals(""))
            throw new Exception("Grpc Server Url is empty!");

        ServicePluginConfig wrapper = new ServicePluginConfig(abSdk);
        wrapper.updateServicePluginConfig(UpdateServicePluginConfig.builder()
                .namespace(abNamespace)
                .body(ServicePluginConfigUpdate.builder()
                        .grpcServerAddress(abGrpcServerUrl)
                        .build())
                .build());
    }

    public void unsetPlatformServiceGrpcTarget() throws Exception {
        ServicePluginConfig wrapper = new ServicePluginConfig(abSdk);
        wrapper.deleteServicePluginConfig(DeleteServicePluginConfig.builder()
                .namespace(abNamespace)
                .build());
    }

    public void deleteStore() throws Exception {
        if (storeId.equals(""))
            return;

        Store storeWrapper = new Store(abSdk);
        storeWrapper.deleteStore(DeleteStore.builder()
                .namespace(abNamespace)
                .storeId(storeId)
                .build());
    }
}