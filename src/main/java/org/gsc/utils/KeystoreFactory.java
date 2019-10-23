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

package org.gsc.utils;

import com.beust.jcommander.JCommander;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.gsc.crypto.ECKey;
import org.gsc.core.Constant;
import org.gsc.config.args.Args;
import org.gsc.keystore.CipherException;
import org.gsc.keystore.Credentials;
import org.gsc.keystore.WalletUtils;

@Slf4j(topic = "app")
public class KeystoreFactory {

    private static final String FilePath = "Wallet";

    private boolean priKeyValid(String priKey) {
        if (StringUtils.isEmpty(priKey)) {
            logger.warn("Warning: PrivateKey is empty !!");
            return false;
        }
        if (priKey.length() != 64) {
            logger.warn("Warning: PrivateKey length need 64 but " + priKey.length() + " !!");
            return false;
        }
        //Other rule;
        return true;
    }

    private void genKeystore() throws CipherException, IOException {
        String password = WalletUtils.inputPassword2Twice();

        ECKey eCkey = new ECKey(Utils.random);
        File file = new File(FilePath);
        if (!file.exists()) {
            if (!file.mkdir()) {
                throw new IOException("Make directory faild!");
            }
        } else {
            if (!file.isDirectory()) {
                if (file.delete()) {
                    if (!file.mkdir()) {
                        throw new IOException("Make directory faild!");
                    }
                } else {
                    throw new IOException("File is exists and can not delete!");
                }
            }
        }
        String fileName = WalletUtils.generateWalletFile(password, eCkey, file, true);
        System.out.println("Generate a keystore its name " + fileName);
        Credentials credentials = WalletUtils.loadCredentials(password, new File(file, fileName));
        System.out.println("Your address is " + credentials.getAddress());
    }

    private void importPrivatekey() throws CipherException, IOException {
        Scanner in = new Scanner(System.in);
        String privateKey;
        System.out.println("Please input private key.");
        while (true) {
            String input = in.nextLine().trim();
            privateKey = input.split("\\s+")[0];
            if (priKeyValid(privateKey)) {
                break;
            }
            System.out.println("Invalid private key, please input again.");
        }

        String password = WalletUtils.inputPassword2Twice();

        ECKey eCkey = ECKey.fromPrivate(ByteArray.fromHexString(privateKey));
        File file = new File(FilePath);
        if (!file.exists()) {
            if (!file.mkdir()) {
                throw new IOException("Make directory faild!");
            }
        } else {
            if (!file.isDirectory()) {
                if (file.delete()) {
                    if (!file.mkdir()) {
                        throw new IOException("Make directory faild!");
                    }
                } else {
                    throw new IOException("File is exists and can not delete!");
                }
            }
        }
        String fileName = WalletUtils.generateWalletFile(password, eCkey, file, true);
        System.out.println("Generate a keystore files:  " + fileName);
        Credentials credentials = WalletUtils.loadCredentials(password, new File(file, fileName));
        System.out.println("Your address is " + credentials.getAddress());
    }

    private void help() {
        System.out.println("You can enter the following command: ");
        System.out.println("Generate Keystore");
        System.out.println("Import Private key");
        System.out.println("Exit or Quit");
        System.out.println("Input any one of then, you will get more tips.");
    }

    private void run() {
        Scanner in = new Scanner(System.in);
        help();
        while (in.hasNextLine()) {
            try {
                String cmdLine = in.nextLine().trim();
                String[] cmdArray = cmdLine.split("\\s+");
                // split on trim() string will always return at the minimum: [""]
                String cmd = cmdArray[0];
                if ("".equals(cmd)) {
                    continue;
                }
                String cmdLowerCase = cmd.toLowerCase();

                switch (cmdLowerCase) {
                    case "help": {
                        help();
                        break;
                    }
                    case "Generate Keystore": {
                        genKeystore();
                        break;
                    }
                    case "Import Private key": {
                        importPrivatekey();
                        break;
                    }
                    case "exit":
                    case "quit": {
                        System.out.println("Exit !!!");
                        in.close();
                        return;
                    }
                    default: {
                        System.out.println("Invalid cmd: " + cmd);
                        help();
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        Args.setParam(args, Constant.MAIN_NET_CONF);
        KeystoreFactory cli = new KeystoreFactory();

        JCommander.newBuilder()
                .addObject(cli)
                .build()
                .parse(args);

        cli.run();
    }
}