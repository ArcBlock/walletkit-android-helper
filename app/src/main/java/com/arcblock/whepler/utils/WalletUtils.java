package com.arcblock.whepler.utils;

import androidx.annotation.Nullable;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.util.List;
import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.wallet.DeterministicSeed;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.protocol.ObjectMapperFactory;

public class WalletUtils {

  private static ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();
  private static final SecureRandom secureRandom = SecureRandomUtils.secureRandom();
  private Credentials credentials;
  public static String ETH_JAXX_TYPE = "m/44'/60'/0'/0/0";
  public static String ETH_LEDGER_TYPE = "m/44'/60'/0'/0";
  public static String ETH_CUSTOM_TYPE = "m/44'/60'/1'/0/0";

  public static DeterministicSeed generateDS() {
    String passphrase = "";
    long creationTimeSeconds = System.currentTimeMillis() / 1000;
    DeterministicSeed ds =
        new DeterministicSeed(secureRandom, 128, passphrase, creationTimeSeconds);
    return ds;
  }

  /**
   * @param ds 助记词加密种子
   */
  @Nullable
  public static ECKeyPair generateWalletByMnemonic(DeterministicSeed ds) {
    String[] pathArray = ETH_JAXX_TYPE.split("/");
    //种子
    byte[] seedBytes = ds.getSeedBytes();
    //助记词
    List<String> mnemonic = ds.getMnemonicCode();
    if (seedBytes == null) {
      return null;
    }
    DeterministicKey dkKey = HDKeyDerivation.createMasterPrivateKey(seedBytes);
    for (int i = 1; i < pathArray.length; i++) {
      ChildNumber childNumber;
      if (pathArray[i].endsWith("'")) {
        int number = Integer.parseInt(pathArray[i].substring(0,
            pathArray[i].length() - 1));
        childNumber = new ChildNumber(number, true);
      } else {
        int number = Integer.parseInt(pathArray[i]);
        childNumber = new ChildNumber(number, false);
      }
      dkKey = HDKeyDerivation.deriveChildKey(dkKey, childNumber);
    }
    ECKeyPair keyPair = ECKeyPair.create(dkKey.getPrivKeyBytes());
    return keyPair;
  }

  private static String convertMnemonicList(List<String> mnemonics) {
    StringBuilder sb = new StringBuilder();
    for (String mnemonic : mnemonics
    ) {
      sb.append(mnemonic);
      sb.append(" ");
    }
    return sb.toString();
  }
}
