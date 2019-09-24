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

package org.gsc.core.wrapper.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.Getter;
import org.gsc.utils.Sha256Hash;

@Getter
public class MerkleTree {

    private static volatile MerkleTree instance;
    private List<Sha256Hash> hashList;
    private List<Leaf> leaves;
    private Leaf root;

    public static MerkleTree getInstance() {
        if (instance == null) {
            synchronized (MerkleTree.class) {
                if (instance == null) {
                    instance = new MerkleTree();
                }
            }
        }
        return instance;
    }

    public MerkleTree createTree(List<Sha256Hash> hashList) {
        this.leaves = new ArrayList<>();
        this.hashList = hashList;
        List<Leaf> leaves = createLeaves(hashList);

        while (leaves.size() > 1) {
            leaves = createParentLeaves(leaves);
        }

        this.root = leaves.get(0);
        return this;
    }

    private List<Leaf> createParentLeaves(List<Leaf> leaves) {
        int step = 2;
        int len = leaves.size();
        return IntStream.iterate(0, i -> i + step)
                .limit(len)
                .filter(i -> i < len)
                .mapToObj(i -> {
                    Leaf right = i + 1 < len ? leaves.get(i + 1) : null;
                    return createLeaf(leaves.get(i), right);
                }).collect(Collectors.toList());
    }

    private List<Leaf> createLeaves(List<Sha256Hash> hashList) {
        int step = 2;
        int len = hashList.size();
        return IntStream.iterate(0, i -> i + step)
                .limit(len)
                .filter(i -> i < len)
                .mapToObj(i -> {
                    Leaf right = i + 1 < len ? createLeaf(hashList.get(i + 1)) : null;
                    return createLeaf(createLeaf(hashList.get(i)), right);
                }).collect(Collectors.toList());
    }

    private Leaf createLeaf(Leaf left, Leaf right) {
        Leaf leaf = new Leaf();
        leaf.hash = right == null ? left.hash : computeHash(left.hash, right.hash);
        leaf.left = left;
        leaf.right = right;
        this.leaves.add(leaf);
        return leaf;
    }

    private Leaf createLeaf(Sha256Hash hash) {
        Leaf leaf = new Leaf();
        leaf.hash = hash;
        this.leaves.add(leaf);
        return leaf;
    }

    private Sha256Hash computeHash(Sha256Hash leftHash, Sha256Hash rightHash) {
        return Sha256Hash.of(leftHash.getByteString().concat(rightHash.getByteString()).toByteArray());
    }

    @Getter
    public class Leaf {

        private Sha256Hash hash;
        private Leaf left, right;
    }
}