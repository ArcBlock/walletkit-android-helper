package com.arcblock.whepler.utils;

import com.google.common.io.BaseEncoding;
import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import forge_abi.Enum;
import forge_abi.Type;
import io.arcblock.walletkit.did.HashType;
import io.arcblock.walletkit.did.KeyType;
import io.arcblock.walletkit.hash.ArcKeccakf1600Hasher;
import io.arcblock.walletkit.hash.ArcSha3Hasher;
import io.arcblock.walletkit.signer.Signer;

/**
 * Created by Nate Gu on 2019/2/27
 */
public class ArcSenderUtils {

  public static String genTxString(ByteString innerTx, String typeUrl, String chainId, String from,
      HashType hashType, KeyType keyType, byte[] sk, byte[] pk) {
    return genTxString(innerTx, typeUrl, chainId, from, hashType, keyType, sk, pk, false);
  }

  public static String genTxString(ByteString innerTx, String typeUrl, String chainId, String from,
      HashType hashType, KeyType keyType, byte[] sk, byte[] pk, boolean forPoke) {

    Any any = Any.newBuilder()
        .setValue(innerTx)
        .setTypeUrl(typeUrl)
        .buildPartial();

    Type.Transaction tx = Type.Transaction.newBuilder()
        .setChainId(chainId)
        .setFrom(from)
        .setItx(any)
        .setPk(ByteString.copyFrom(pk))
        .setNonce(forPoke ? 0 : System.currentTimeMillis())
        .buildPartial();

    byte[] contentHash = null;

    int hashTypeValue = hashType.getValue();

    if (hashTypeValue == HashType.KECCAK.getValue()) {
      contentHash = ArcKeccakf1600Hasher.sha256(tx.toByteArray(), 1);
    } else if (hashTypeValue == HashType.SHA3.getValue()) {
      contentHash = ArcSha3Hasher.sha256(tx.toByteArray(), 1);
    } else if (hashTypeValue == HashType.KECCAK_384.getValue()) {
      contentHash = ArcKeccakf1600Hasher.sha384(tx.toByteArray(), 1);
    } else if (hashTypeValue == HashType.SHA3_384.getValue()) {
      contentHash = ArcKeccakf1600Hasher.sha384(tx.toByteArray(), 1);
    } else if (hashTypeValue == HashType.KECCAK_512.getValue()) {
      contentHash = ArcKeccakf1600Hasher.sha384(tx.toByteArray(), 1);
    } else if (hashTypeValue == HashType.SHA3_512.getValue()) {
      contentHash = ArcKeccakf1600Hasher.sha384(tx.toByteArray(), 1);
    } else {
      contentHash = new byte[] {};
    }

    byte[] signature = Signer.INSTANCE.sign(keyType, contentHash, sk);
    tx = tx.toBuilder().setSignature(ByteString.copyFrom(signature)).buildPartial();
    return BaseEncoding.base64Url().encode(tx.toByteArray());
  }
}
