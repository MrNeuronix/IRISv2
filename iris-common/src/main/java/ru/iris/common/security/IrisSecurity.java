package ru.iris.common.security;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.time.DateUtils;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.UUID;

/**
 * Class for handling security related operations
 */
public class IrisSecurity {
    /** The logger. */
    private static Logger LOGGER = LoggerFactory.getLogger(IrisSecurity.class);
    /** The security provider. */
    private final static Provider provider = new BouncyCastleProvider();
    static {
        Security.addProvider(provider);
    }

    private final UUID instanceId;
    private final String keystorePassword;
    private final String keystorePath;
    private final KeyStore keystore;

    public IrisSecurity(final UUID instanceId, final String keystorePath, final String keystorePassword) {
        this.instanceId = instanceId;
        this.keystorePath = keystorePath;
        this.keystorePassword = keystorePassword;
        try {
            // Create keystore if it is missing
            if (!new File(keystorePath).exists()) {
                final KeyStore keystore = KeyStore.getInstance("JKS");
                keystore.load(null, keystorePassword.toCharArray());

                final FileOutputStream fileOutputStream = new FileOutputStream(keystorePath, false);
                keystore.store(fileOutputStream, keystorePassword.toCharArray());
                fileOutputStream.close();
            }

            final File keystoreFile = new File(keystorePath);
            final FileInputStream keystoreInputStream = new FileInputStream(keystoreFile);
            keystore = KeyStore.getInstance("JKS");
            keystore.load(keystoreInputStream, keystorePassword.toCharArray());

            // If own certificate is missing from the keystore then generate certificate.
            if (!keystore.containsAlias(instanceId.toString())) {
                // Acquire exclusive lock to keystore for the period of loading and possible updating of the keystore.
                final FileOutputStream keystoreOutputStream = new FileOutputStream(keystoreFile, false);
                final FileChannel keystoreChannel = keystoreOutputStream.getChannel();
                FileLock keystoreLock = null;
                while ((keystoreLock = keystoreChannel.tryLock()) == null) {
                    Thread.sleep(100);
                }

                final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", provider);
                final KeyPair keyPair = keyGen.generateKeyPair();
                final X509Certificate certificate = buildCertificate(instanceId.toString(), keyPair);
                keystore.setKeyEntry(instanceId.toString(), (Key)keyPair.getPrivate(), keystorePassword.toCharArray(),
                        new X509Certificate[] {certificate});

                keystore.store(keystoreOutputStream, keystorePassword.toCharArray());
                keystoreLock.release();
                keystoreOutputStream.close();
            }

            keystoreInputStream.close();

        } catch (final Exception e) {
            throw new RuntimeException("Error initializing security.", e);
        }
    }

    /**
     * Calculates signature for message string using certificate identified by instance ID.
     * @param message the message string
     * @return the signature
     */
    public String calculateSignature(final String message) {
        try {
            final PrivateKey privateKey =
                    (PrivateKey) keystore.getKey(instanceId.toString(), keystorePassword.toCharArray());
            final Signature signature = Signature.getInstance("SHA256withRSA", provider);
            signature.initSign(privateKey);
            signature.update(message.getBytes("UTF-8"));
            LOGGER.debug("Signature calculated for: " + message);
            return Hex.encodeHexString(signature.sign());
        } catch (final Exception e) {
            throw new RuntimeException("Error calculating signature for message.", e);
        }
    }

    /**
     * Verifies that signature has been created by given remote instance ID.
     * @param message the message
     * @param signatureString the signature
     * @param remoteInstanceId
     * @return true if signature is valid
     */
    public boolean verifySignature(final String message, final String signatureString, final UUID remoteInstanceId) {
        try {
            final X509Certificate certificate = (X509Certificate) keystore.getCertificate(remoteInstanceId.toString());
            if (certificate == null) {
                LOGGER.warn("Unknown certificate: " + remoteInstanceId);
                return false;
            }
            if (!certificate.getSubjectDN().toString().equals("CN="+remoteInstanceId)) {
                LOGGER.warn("Invalid certificate DN: '" + certificate.getSubjectDN()
                        + "' for certificate alias: '" + remoteInstanceId + "'");
                return false;
            }
            final PublicKey publicKey = certificate.getPublicKey();
            final Signature signature = Signature.getInstance("SHA256withRSA", provider);
            signature.initVerify(publicKey);
            signature.update(message.getBytes("UTF-8"));
            if (signature.verify(Hex.decodeHex(signatureString.toCharArray()))) {
                return true;
            } else {
                LOGGER.warn("Signature verification failed for: " + message);
                return false;
            }
        } catch (final Exception e) {
            throw new RuntimeException("Error verifying signature for message.", e);
        }
    }

    /**
     * Encrypt the plain text using public key indicated by the remote instance ID.
     *
     * @param plainText plain text
     * @param remoteInstanceId the remote instance ID
     * @return cipher text
     */
    public String encrypt(String plainText, final UUID remoteInstanceId) {
        try {
            final X509Certificate certificate = (X509Certificate) keystore.getCertificate(remoteInstanceId.toString());
            if (certificate == null) {
                throw new RuntimeException("Unknown certificate: " + remoteInstanceId);
            }
            if (!certificate.getSubjectDN().toString().equals("CN="+remoteInstanceId)) {
                throw new RuntimeException("Invalid certificate DN: '" + certificate.getSubjectDN()
                        + "' for certificate alias: '" + remoteInstanceId + "'");
            }
            final PublicKey publicKey = certificate.getPublicKey();
            final Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            byte[] cipherText = cipher.doFinal(plainText.getBytes("UTF-8"));
            return Hex.encodeHexString(cipherText);
        } catch (final Exception e) {
            throw new RuntimeException("Error encrypting plain text.", e);
        }
    }

    /**
     * Decrypts cipher text with this instances private key.
     * @param cipherText the cipher text
     * @return the plain text
     */
    public String decrypt(String cipherText) {
        try {
            final PrivateKey privateKey = (PrivateKey) keystore.getKey(
                    instanceId.toString(), keystorePassword.toCharArray());
            final Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, privateKey);
            byte[] plainText = cipher.doFinal(Hex.decodeHex(cipherText.toCharArray()));
            return new String(plainText, "UTF-8");
        } catch (final Exception e) {
            throw new RuntimeException("Error decrypting cipher text.", e);
        }
    }

    /**
     * Build self signed certificate from key pair.
     * @param commonName the certificate common name
     * @param keyPair the key pair.
     * @return the certificate
     * @throws Exception if error occurs in certificate generation process.
     */
    private X509Certificate buildCertificate(final String commonName, KeyPair keyPair) throws Exception {

        final Date notBefore = new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24);
        final Date notAfter = DateUtils.addYears(notBefore, 100);
        final BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());

        final X500NameBuilder nameBuilder = new X500NameBuilder(BCStyle.INSTANCE);
        nameBuilder.addRDN(BCStyle.CN, commonName);
        final SubjectPublicKeyInfo subjectPublicKeyInfo = new SubjectPublicKeyInfo(
                ASN1Sequence.getInstance(keyPair.getPublic().getEncoded()));

        final X509v3CertificateBuilder certGen = new X509v3CertificateBuilder(nameBuilder.build(),
                serial, notBefore, notAfter, nameBuilder.build(), subjectPublicKeyInfo);
        final ContentSigner sigGen = new JcaContentSignerBuilder("SHA256WithRSAEncryption")
                .setProvider(provider).build(keyPair.getPrivate());
        final X509Certificate cert = new JcaX509CertificateConverter().setProvider(provider)
                .getCertificate(certGen.build(sigGen));

        return cert;
    }
}
