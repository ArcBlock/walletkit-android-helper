package com.arcblock.whepler.utils

import com.google.crypto.tink.subtle.Ed25519Sign
import io.arcblock.walletkit.did.DidKeyPair
import io.arcblock.walletkit.did.HashType
import io.arcblock.walletkit.did.HashType.SHA2
import io.arcblock.walletkit.did.HashType.SHA3
import io.arcblock.walletkit.did.HashUtils
import io.arcblock.walletkit.did.KeyType
import io.arcblock.walletkit.did.KeyType.ED25519
import io.arcblock.walletkit.did.KeyType.SECP256K1
import io.arcblock.walletkit.did.RoleType
import io.arcblock.walletkit.did.RoleType.ACCOUNT
import io.arcblock.walletkit.hash.ArcSha2Hasher
import io.arcblock.walletkit.hash.ArcSha3Hasher
import io.arcblock.walletkit.hash.Hasher
import io.arcblock.walletkit.utils.Base58Btc
import io.arcblock.walletkit.utils.hash
import org.bitcoinj.core.Base58
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.crypto.HDKeyDerivation
import org.web3j.crypto.ECKeyPair
import java.math.BigInteger

/**
 * Author       :paperhuang
 * Time         :2019/2/14
 * Edited By    :
 * Edited Time  :
 **/
object IdGenerator {

  /**
   * @see <a href="https://github.com/ArcBlock/ABT-DID-Protocol">link</a>
   *
   * Apply sha3 to the app_did
  Take the first 64 bits of the hash
  Split the these 64 bits into two 32-bits-long sections denoted as S1 and S2.
  Derive the HD secret key by using path m/44'/ABT'/S1'/S2'/address_index where ABT' is the coin type registered on SLIP44 and address_index is numbered from index 0 in sequentially increasing manner.
  Convert the HD secret key to user_did by using the rules described in DID section.
  From this point, the wallet should use this derived secret key, public key and DID for future processing.
   *
   */
  fun genAppDid(didKeyPair: DidKeyPair): String {
    // Convert the HD secret key to user_did by using the rules described in DID section.
    return IdGenerator.sk2did(didKeyPair.privateKey)
  }

  fun genAppKeyPair(appid: String, index: Int, seed: ByteArray, keyType: KeyType): DidKeyPair {
    // Apply sha3 to the app_did
    val sha3out = ArcSha3Hasher.sha256(Base58Btc.decode(appid.removePrefix("did:abt:")), 1)

    //    Take the first 64 bits of the hash
    //    Split the these 64 bits into two 32-bits-long sections denoted as S1 and S2.
    val S1 = BigInteger(sha3out.sliceArray(0..3)) // big-endian by default
    val S2 = BigInteger(sha3out.sliceArray(4..7)) // big-endian by default
    val S0 = 260 //ABT
    //    Derive the HD secret key by using path m/44'/ABT'/S1'/S2'/address_index where ABT' is the coin type registered on SLIP44 and address_index is numbered from index 0 in sequentially increasing manner.
    val derivePath = "m/44'/$S0'/$S1'/$S2'/$index"
    val pathArray = derivePath.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    var dkKey = HDKeyDerivation.createMasterPrivateKey(seed)
    for (i in 1 until pathArray.size) {
      val childNumber: ChildNumber
      if (pathArray[i].endsWith("'")) {
        val number = Integer.parseInt(pathArray[i].removeSuffix("'"))
        childNumber = ChildNumber(number.and(0x7FFFFFFF), true)
      } else {
        val number = Integer.parseInt(pathArray[i])
        childNumber = ChildNumber(number.and(0x7FFFFFFF), false)
      }
      dkKey = HDKeyDerivation.deriveChildKey(dkKey, childNumber)
    }
    return DidKeyPair(keyType, dkKey.privKeyBytes)
  }

  /**
   *
  Step 1: Choose the RoleType, KeyType and Hash from above, let's use application, ed25519 and sha3 in this example.
  Step 2: Choose a secret key randomly, e.g.
  D67C071B6F51D2B61180B9B1AA9BE0DD0704619F0E30453AB4A592B036EDE644E4852B7091317E3622068E62A5127D1FB0D4AE2FC50213295E10652D2F0ABFC7
  Step 3: Generate the public key of this secret key by using the KeyType. So we can get public key
  E4852B7091317E3622068E62A5127D1FB0D4AE2FC50213295E10652D2F0ABFC7
  Step 4: Get the Hash of the public key
  EC8E681514753FE5955D3E8B57DAEC9D123E3DB146BDDFC3787163F77F057C27
  Step 5: Take the first 20 bytes of the public key hash
  EC8E681514753FE5955D3E8B57DAEC9D123E3DB1
  Step 6: Add the DID type bytes 0x0C01 in front of the hash of Step 4
  0C01EC8E681514753FE5955D3E8B57DAEC9D123E3DB1
  Step 7: Get the hash of the extended hash in Step 6
  42CD815145538F8003586C880AF94418341F9C4B8FA0394876553F8A952C7D03
  Step 8: Take the first 4 bytes in step 7
  42CD8151
  Step 9: Append the 4 bytes in step 8 to the extended hash in step 6. This is the binary DID string
  0C01EC8E681514753FE5955D3E8B57DAEC9D123E3DB142CD8151
  Step 10: Encode the binary value by using the Bitcoin Base58 method.
  zNKtCNqYWLYWYW3gWRA1vnRykfCBZYHZvzKr
  Step 11: Assemble the parts and get the full DID
  did:abt:zNKtCNqYWLYWYW3gWRA1vnRykfCBZYHZvzKr
   */
  fun sk2did(sk: ByteArray): String {
    return IdGenerator.sk2did(ACCOUNT, ED25519, SHA3, sk)
  }

  fun sk2did(roleType: RoleType, keyType: KeyType, hashType: HashType, sk: ByteArray): String {
    val pk = sk2pk(keyType, sk)
    return pk2did(roleType, keyType, hashType, pk)
  }

  fun pk2did(roleType: RoleType, keyType: KeyType, hashType: HashType, pk: ByteArray): String {
    val pkHash = hash(hashType, pk).sliceArray(0..19)
    val appendPk = preAppend(roleType, keyType, hashType) + pkHash
    val suffix = appendPk + hash(hashType, appendPk).sliceArray(0..3)
    return "did:abt:z".plus(Base58.encode(suffix))
  }

  fun preAppend(roleType: RoleType, keyType: KeyType, hashType: HashType): ByteArray {
    val append = (roleType.value.shl(10).and(0b1111110000000000)).or(keyType.ordinal.shl(5).and(0b1111100000)).or(hashType.ordinal.and(0b11111))
    var ret = ByteArray(2)
    ret[1] = append.and(0b11111111).toByte()
    ret[0] = append.shr(8).and(0b11111111).toByte()
    return ret
  }

  fun hash(hashType: HashType, pk: ByteArray): ByteArray {
    return when (hashType) {
      SHA3 -> {
        ArcSha3Hasher.sha256(pk, 1)
      }
      else -> {
        HashUtils.sha3(pk)
      }
    }
  }

  /**
   * Generate address by user publicKey
   * @param roleType use enum RoleType, such as ACCOUNT or APPLICATION
   * @param keyType publicKey type ED25519 or SECP256K1
   * @param hashType enum HashType, such as SHA3 or KECCAK
   */
  fun pk2Address(roleType: RoleType, keyType: KeyType = ED25519, hashType: HashType = SHA3, pk: ByteArray): String {
    val pkHash = if (hashType == SHA2) ArcSha2Hasher.sha256(pk, 1) else Hasher.hash(hashType, pk)
    return hashToAddress(roleType, keyType, hashType, pkHash)
  }

  fun hashToAddress(roleType: RoleType, keyType: KeyType = ED25519, hashType: HashType = SHA3, hash: ByteArray): String {
    val appendPk = preAppend(roleType, keyType, hashType) + hash.sliceArray(0..19)
    val suffix = appendPk + Hasher.hash(hashType, appendPk).sliceArray(0..3)
    return Base58Btc.encode(suffix)
  }

  /**
   * generate tether address
   */
  fun toTetherAddress(hash: ByteArray): String {
    return hashToAddress(RoleType.TETHER, ED25519, SHA2, hash)
  }

  /**
   * generate validator address
   */
  fun toValidatorAddress(hash: ByteArray): String {
    return hashToAddress(RoleType.VALIDATOR, ED25519, SHA2, hash)
  }

  /**
   * generate node address
   */
  fun toNodeAddress(hash: ByteArray): String {
    return hashToAddress(RoleType.NODE, ED25519, SHA2, hash)
  }

  fun toSwapAddress(hash: ByteArray): String {
    return hashToAddress(RoleType.SWAP, ED25519, SHA2, hash = hash)
  }

  /**
   * Generate stake address. Use sender's address + receiver's address as pseudo public key.
   * Use `ed25519` as pseudo key type. Use sha3 and base58 by default.
   */
  fun toStakeAddress(addr1: String, addr2: String): String {
    val data = (if (addr1.compareTo(addr2) < 0) {
      addr1 + addr2
    } else addr2 + addr1).toByteArray()
    return pk2Address(RoleType.STAKE, ED25519, SHA3, data)
  }

  /**
   *   Generate address for tx.
   */
  fun toTxAddress(itx: ByteArray): String {
    val data = Hasher.hash(SHA3, itx)
    return hashToAddress(RoleType.TX, ED25519, SHA3, data)
  }

  fun sk2pk(keyType: KeyType, sk: ByteArray): ByteArray {

    return when (keyType) {
      ED25519 -> {
        val signer = Ed25519Sign(sk.sliceArray(0..31))
        val pkField = signer.javaClass.getDeclaredField("publicKey")
        pkField.isAccessible = true
        pkField.get(signer) as ByteArray
      }
      SECP256K1 -> {
        ECKeyPair.create(sk.sliceArray(0..31)).publicKey.toByteArray().let {
          it[0] = 4
          it
        }
      }
    }
  }

  /**
   * Generate delegateDID
   */
  fun genDelegateAddress(sender: String, receiver: String): String {
    val data = sender.toByteArray() + receiver.toByteArray()
    return hashToAddress(RoleType.DELEGATE, hash = data.hash(SHA3))
  }

}
