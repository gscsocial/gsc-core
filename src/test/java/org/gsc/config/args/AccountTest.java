package org.gsc.config.args;

import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.gsc.common.utils.ByteArray;
import org.gsc.config.Parameter;
import org.gsc.protos.Protocol;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@Slf4j
public class AccountTest {

    private Account account = new Account();

    @Before
    public void setAccount() {
        account.setAccountName("allen");
        account.setAccountType("Normal");
        account.setAddress(ByteArray.fromHexString(
                Parameter.WalletConstant.ADD_PRE_FIX_STRING_MAINNET + "a901c2e8a756d9437037dcd8c7e0c73d560c14e2"));
        account.setBalance("999");
    }
    @Test(expected = IllegalArgumentException.class)
    public void whenSetNullAccountNameShouldThrowIllegalArgumentException() {
        account.setAccountName(null);
    }


    @Test(expected = IllegalArgumentException.class)
    public void whenSetEmptyAccountNameShouldThrowIllegalArgumentException() {
        account.setAccountName("");
    }

    @Test
    public void setAccountNameRight() {
        account.setAccountName("allen");

        byte[] bytes = ByteArray.fromString("allen");

        if (ArrayUtils.isNotEmpty(bytes)) {
            ByteString accountName = ByteString.copyFrom(bytes);
            Assert.assertEquals(accountName, account.getAccountName());
        }
    }

    @Test
    public void getAccountName() {
        byte[] bytes = ByteArray.fromString("allen");

        if (ArrayUtils.isNotEmpty(bytes)) {
            ByteString accountName = ByteString.copyFrom(bytes);
            Assert.assertEquals(accountName, account.getAccountName());
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenSetNullAccountTypeShouldThrowIllegalArgumentException() {
        account.setAccountType(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenSetEmptyAccountTypeShouldThrowIllegalArgumentException() {
        account.setAccountType("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenSetOtherAccountTypeShouldThrowIllegalArgumentException() {
        account.setAccountType("other");
    }

    @Test
    public void setAccountTypeRight() {
        account.setAccountType("ASSETISSUE");

        Assert.assertEquals(account.getAccountTypeByString("ASSETISSUE"), account.getAccountType());
    }

    @Test
    public void getAccountType() {
        Assert.assertEquals(account.getAccountTypeByString("Normal"), account.getAccountType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenSetNullAddressShouldThrowIllegalArgumentException() {
        account.setAddress(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenSetEmptyAddressShouldThrowIllegalArgumentException() {
        account.setAddress(new byte[0]);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenSetBadFormatAddressShouldThrowIllegalArgumentException() {
        account.setAddress(ByteArray.fromHexString("3452123"));
    }

    @Test
    public void getAddress() {
        Assert.assertEquals(
                Parameter.WalletConstant.ADD_PRE_FIX_STRING_MAINNET + "4948c2e8a756d9437037dcd8c7e0c73d560ca38d",
                ByteArray.toHexString(account.getAddress()));
    }

    @Test
    public void getAddressBytes() {
        byte[] bytes = ByteArray.fromHexString(
                Parameter.WalletConstant.ADD_PRE_FIX_STRING_MAINNET + "a901c2e8a756d9437037dcd8c7e0c73d560c14e2");
        Assert.assertArrayEquals(bytes, account.getAddress());
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenSetNullBalanceShouldThrowIllegalArgumentException() {
        account.setBalance(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenSetEmptyBalanceShouldThrowIllegalArgumentException() {
        account.setBalance("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenSetNonDigitalBalanceShouldThrowIllegalArgumentException() {
        account.setBalance("abc");
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenSetExceedTheMaxBalanceShouldThrowIllegalArgumentException() {
        account.setBalance("19223372036854775808");
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenSetExceedTheMinBalanceShouldThrowIllegalArgumentException() {
        account.setBalance("-29223372036854775809");
    }

    @Test
    public void setMaxBalanceRight() {
        account.setBalance("9223372036854775807");
        Assert.assertEquals(Long.MAX_VALUE, account.getBalance());
    }

    @Test
    public void setMinBalanceRight() {
        account.setBalance("-9223372036854775808");
        Assert.assertEquals(Long.MIN_VALUE, account.getBalance());
    }

    @Test
    public void setBalanceRight() {
        account
                .setAddress(ByteArray.fromHexString(
                        Parameter.WalletConstant.ADD_PRE_FIX_STRING_MAINNET + "92814a458256d9437037dcd8c7e0c7948327154d"));
        Assert.assertEquals(
                Parameter.WalletConstant.ADD_PRE_FIX_STRING_MAINNET + "92814a458256d9437037dcd8c7e0c7948327154d",
                ByteArray.toHexString(account.getAddress()));
    }

    @Test
    public void getBalance() {
        Assert.assertEquals(10000, account.getBalance());
    }

    @Test
    public void testIsAccountType() {
        Assert.assertFalse(account.isAccountType(null));
        Assert.assertFalse(account.isAccountType(""));
        Assert.assertFalse(account.isAccountType("123"));
        Assert.assertTrue(account.isAccountType("Normal"));
        Assert.assertTrue(account.isAccountType("normal"));
        Assert.assertTrue(account.isAccountType("AssetIssue"));
        Assert.assertTrue(account.isAccountType("assetissue"));
        Assert.assertTrue(account.isAccountType("Contract"));
        Assert.assertTrue(account.isAccountType("contract"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenGetNullAccountTypeByStringShouldThrowIllegalArgumentException() {
        Assert.assertEquals(Protocol.AccountType.Normal, account.getAccountTypeByString(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenGetEmptyAccountTypeByStringShouldThrowIllegalArgumentException() {
        Assert.assertEquals(Protocol.AccountType.Normal, account.getAccountTypeByString(""));
    }

    @Test
    public void testGetAccountTypeByStringRight() {
        Assert.assertEquals(Protocol.AccountType.Normal, account.getAccountTypeByString("Normal"));
        Assert.assertEquals(Protocol.AccountType.Normal, account.getAccountTypeByString("normal"));
        Assert.assertEquals(Protocol.AccountType.AssetIssue, account.getAccountTypeByString("AssetIssue"));
        Assert.assertEquals(Protocol.AccountType.AssetIssue, account.getAccountTypeByString("assetissue"));
        Assert.assertEquals(Protocol.AccountType.Contract, account.getAccountTypeByString("Contract"));
        Assert.assertEquals(Protocol.AccountType.Contract, account.getAccountTypeByString("contract"));
    }
}
