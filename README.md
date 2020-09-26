## ArcBlock WalletKit Android Helper

This repo will show you how to create a wallet with ArcBlock WalletKit.

## Step 1: Config Dependencies

Add maven repository

```
allprojects {
  repositories {
    ....
    maven {
      url "http://android-docs.arcblock.io/release"
    }
  }
}
```

then add dependencies

```
implementation("com.google.protobuf:protobuf-java:3.6.1")
implementation("com.arcblock.corekit:absdkcorekit:0.4.1"){
  exclude module: "jsr305"
  exclude module: "jsr250-api"
  exclude module: 'jackson-core'
  exclude module: 'jackson-annotations'
  exclude module: 'jackson-databind'
}
implementation("io.arcblock.walletkit:chainkit:0.4.14")
implementation("io.arcblock.walletkit:walletkit:0.4.15") {
  exclude module: 'protobuf-lite'
  exclude group: 'com.google.protobuf'
  exclude module: 'jackson-core'
  exclude module: 'jackson-annotations'
  exclude module: 'jackson-databind'
  exclude group: 'org.ow2.asm'
}
```

add packagingOptions

> This will fix INSTALL_FAILED_NO_MATCHING_ABIS error

```
android{
  ...
  packagingOptions {
    exclude 'lib/x86_64/darwin/libscrypt.dylib'
    exclude 'lib/x86_64/freebsd/libscrypt.so'
    exclude 'lib/x86_64/linux/libscrypt.so'
  }
}
```

## Step2: Config AndroidManifest.xml

```
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```

## Step3: View The Demo Code

[Demo Code](./app/src/main/java/com/arcblock/whepler/MainActivity.kt)
