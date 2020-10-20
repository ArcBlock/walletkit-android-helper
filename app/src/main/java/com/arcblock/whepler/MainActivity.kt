package com.arcblock.whepler

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.apollographql.apollo.rx2.Rx2Apollo
import com.arcblock.corekit.ABCoreKitClient
import com.arcblock.wallet.appcommonsdk.SendTxMutation
import com.arcblock.whepler.utils.ArcSenderUtils
import com.arcblock.whepler.utils.IdGenerator
import com.arcblock.whepler.utils.WalletUtils
import forge_abi.Declare
import io.arcblock.chainkit.ArcWalletClientUtils
import io.arcblock.protobuf.TypeUrls
import io.arcblock.walletkit.did.DidUtils
import io.arcblock.walletkit.did.KeyType.ED25519
import io.arcblock.walletkit.utils.address
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class MainActivity : AppCompatActivity() {
  private val compositeDisposable = CompositeDisposable()
  private lateinit var client: ABCoreKitClient

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    // 1. create seed, this seed will be used to create wallet
    val seed = WalletUtils.generateDS()

    // 2. create did address
    val kp = IdGenerator.genAppKeyPair("", 0, seed.seedBytes!!, ED25519)
    val did = IdGenerator.sk2did(kp.privateKey)
    Log.d("MainActivity", "did:$did")

    // 3. declare the wallet to the chain
    // Note: you can declare the wallet on any chain based on ArcBlock Chain Framework
    // 3.1 create client to send tx, https://zinc.abtnetwork.io/api is our test chain host, your can replace with yours.
    val testChainId = "zinc-2019-05-17"
    val testChainHost = "https://zinc.abtnetwork.io/api"
    client = ArcWalletClientUtils.getApiClient(this, testChainHost)
    // 3.2 create declare tx
    val declareInnerTx = Declare.DeclareTx.newBuilder().setMoniker("TestAccount").buildPartial()
    // 3.3 sign the tx with the wallet key pair
    val walletAddress = did.address()
    val signedTx = ArcSenderUtils.genTxString(
      declareInnerTx.toByteString(),
      TypeUrls.DECLARE,
      testChainId,
      walletAddress,
      DidUtils.decodeDidHashType(walletAddress),
      DidUtils.decodeDidSignType(walletAddress),
      kp.privateKey,
      kp.publicKey
    )
    // 3.4 send the signed declare tx to the chain
    sendTx(signedTx, walletAddress)
  }

  fun sendTx(signedTx: String, walletAddress: String) {
    compositeDisposable.add(
      Rx2Apollo.from(
        client.mutate(
          SendTxMutation.builder().tx(signedTx).build()
        )
      ).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe({
        if (!it.hasErrors()) {
          // 4. Congratulations! You have create a wallet and declare it to the chain
          Toast.makeText(this, "Send success ,Hash:${it.data()?.sendTx?.hash}", Toast.LENGTH_LONG).show()
          Log.d("MainActivity", "You can view the tx on block explorer: https://zinc.abtnetwork.io/node/explorer/txs/${it.data()?.sendTx?.hash}")
          Log.d("MainActivity", "You can view the wallet on block explorer: https://zinc.abtnetwork.io/node/explorer/accounts/${walletAddress}")
        } else {
          Toast.makeText(this, it.errors()[0].message(), Toast.LENGTH_LONG).show()
        }
      }, {
        Log.e("tag", it.message ?: "")
      })
    )
  }
}