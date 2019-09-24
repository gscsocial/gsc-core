/*
 * GSC (Global Social Chain), a blockchain fit for mass adoption and
 * a sustainable token economy model, is the decentralized global social
 * chain with highly secure, low latency, and near-zero fee transactional system.
 *
 * gsc-core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * License GSC-Core is under the GNU General Public License v3. See LICENSE.
 */

package org.gsc.db;

import static org.gsc.config.Parameter.DatabaseConstants.ASSET_ISSUE_COUNT_LIMIT_MAX;

import com.google.common.collect.Streams;

import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.gsc.core.wrapper.AssetIssueWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j(topic = "DB")
@Component
public class AssetIssueStore extends GSCStoreWithRevoking<AssetIssueWrapper> {

    @Autowired
    protected AssetIssueStore(@Value("asset_issue") String dbName) {
        super(dbName);
    }


    @Override
    public AssetIssueWrapper get(byte[] key) {
        return super.getUnchecked(key);
    }

    /**
     * get all asset issues.
     */
    public List<AssetIssueWrapper> getAllAssetIssues() {
        return Streams.stream(iterator())
                .map(Entry::getValue)
                .collect(Collectors.toList());
    }

    private List<AssetIssueWrapper> getAssetIssuesPaginated(List<AssetIssueWrapper> assetIssueList,
                                                            long offset, long limit) {
        if (limit < 0 || offset < 0) {
            return null;
        }

//    return Streams.stream(iterator())
//        .map(Entry::getValue)
//        .sorted(Comparator.comparing(a -> a.getName().toStringUtf8(), String::compareTo))
//        .skip(offset)
//        .limit(Math.min(limit, ASSET_ISSUE_COUNT_LIMIT_MAX))
//        .collect(Collectors.toList());

        if (assetIssueList.size() <= offset) {
            return null;
        }
        assetIssueList.sort((o1, o2) -> {
            if (o1.getName() != o2.getName()) {
                return o1.getName().toStringUtf8().compareTo(o2.getName().toStringUtf8());
            }
            return Long.compare(o1.getOrder(), o2.getOrder());
        });
        limit = limit > ASSET_ISSUE_COUNT_LIMIT_MAX ? ASSET_ISSUE_COUNT_LIMIT_MAX : limit;
        long end = offset + limit;
        end = end > assetIssueList.size() ? assetIssueList.size() : end;
        return assetIssueList.subList((int) offset, (int) end);
    }

    public List<AssetIssueWrapper> getAssetIssuesPaginated(long offset, long limit) {
        return getAssetIssuesPaginated(getAllAssetIssues(), offset, limit);
    }
}
