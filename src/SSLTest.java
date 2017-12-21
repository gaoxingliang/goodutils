
/*
 * SSLTest.java
 *
 * Tests servers for SSL/TLS protocol and cipher support.
 *
 * Copyright (c) 2015 Christopher Schultz
 *
 * Christopher Schultz licenses this file to You under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
// Note this class requires [[SSLUtils.java]]
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * A driver class to test a server's SSL/TLS support.
 *
 * Usage: java SSLTest [opts] host[:port]
 *
 * Try "java SSLTest -h" for help.
 *
 * https://wiki.apache.org/tomcat/tools/SSLTest.java
 *
 * This tester will attempts to handshake with the target host with all
 * available protocols and ciphers and report which ones were accepted and
 * which were rejected. An HTTP connection is never fully made, so these
 * connections should not flood the host's access log with entries.
 *
 * @author Christopher Schultz
 */
public class SSLTest
{
    public static void usage()
    {
        System.out.println("Usage: java " + SSLTest.class + " [opts] host[:port]");
        System.out.println();
        System.out.println("-sslprotocol                 Sets the SSL/TLS protocol to be used (e.g. SSL, TLS, SSLv3, TLSv1.2, etc.)");
        System.out.println("-enabledprotocols protocols  Sets individual SSL/TLS ptotocols that should be enabled");
        System.out.println("-ciphers cipherspec          A comma-separated list of SSL/TLS ciphers");

        System.out.println("-truststore                  Sets the trust store for connections");
        System.out.println("-truststoretype type         Sets the type for the trust store");
        System.out.println("-truststorepassword pass     Sets the password for the trust store");
        System.out.println("-truststorealgorithm alg     Sets the algorithm for the trust store");
        System.out.println("-truststoreprovider provider Sets the crypto provider for the trust store");

        System.out.println("-no-check-certificate        Ignores certificate errors");
        System.out.println("-no-verify-hostname          Ignores hostname mismatches");
        System.out.println("-unlimited-jce               Enable unlimited JCE");
        System.out.println("-h -help --help     Shows this help message");
    }


    public static void main(String[] args)
        throws Exception
    {


        System.out.println("Current AES length - " + JCEUtils.getAESMaxKeyLength());

        int connectTimeout = 0; // default = infinite
        int readTimeout = 1000;

        boolean disableHostnameVerification = true;
        boolean disableCertificateChecking = true;

        String trustStoreFilename = System.getProperty("javax.net.ssl.trustStore");
        String trustStorePassword = System.getProperty("javax.net.ssl.trustStorePassword");
        String trustStoreType = System.getProperty("javax.net.ssl.trustStoreType");
        String trustStoreProvider = System.getProperty("javax.net.ssl.trustStoreProvider");
        String trustStoreAlgorithm = null;
        String sslProtocol = "TLS";
        String[] sslEnabledProtocols = new String[] { "SSLv2", "SSLv2hello", "SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2" };
        String[] sslCipherSuites = null; // Default = default for protocol
        String crlFilename = null;
        boolean showCerts = false;
        boolean unlimitedJCE = false;
        if(args.length < 1)
        {
            usage();
            System.exit(0);
        }

        int argIndex;
        for(argIndex = 0; argIndex < args.length; ++argIndex)
        {
            String arg = args[argIndex];

            if(!arg.startsWith("-"))
                break;
            else if("--".equals(arg))
                break;
            else if("-no-check-certificate".equals(arg))
                disableCertificateChecking = true;
            else if ("-unlimited-jce".equals(arg)) {
                unlimitedJCE = true;
            }
            else if("-no-verify-hostname".equals(arg))
                disableHostnameVerification = true;
            else if("-sslprotocol".equals(arg))
                sslProtocol = args[++argIndex];
            else if("-enabledprotocols".equals(arg))
                sslEnabledProtocols = args[++argIndex].split("\\s*,\\s*");
            else if("-ciphers".equals(arg))
                sslCipherSuites = args[++argIndex].split("\\s*,\\s*");
            else if("-connecttimeout".equals(arg))
                connectTimeout = Integer.parseInt(args[++argIndex]);
            else if("-readtimeout".equals(arg))
                readTimeout = Integer.parseInt(args[++argIndex]);
            else if("-truststore".equals(arg))
                trustStoreFilename = args[++argIndex];
            else if("-truststoretype".equals(arg))
                trustStoreType = args[++argIndex];
            else if("-truststorepassword".equals(arg))
                trustStorePassword = args[++argIndex];
            else if("-truststoreprovider".equals(arg))
                trustStoreProvider = args[++argIndex];
            else if("-truststorealgorithm".equals(arg))
                trustStoreAlgorithm = args[++argIndex];
            else if("-showcerts".equals(arg))
                showCerts = true;
            else if("--help".equals(arg)
                    || "-h".equals(arg)
                    || "-help".equals(arg))
            {
                usage();
                System.exit(0);
            }
            else
            {
                System.err.println("Unrecognized option: " + arg);
                System.exit(1);
            }
        }

        if(argIndex >= args.length)
        {
            System.err.println("Unexpected additional arguments: "
                               + java.util.Arrays.asList(args).subList(argIndex, args.length));

            usage();
            System.exit(1);
        }

        if (unlimitedJCE) {
            JCEUtils.removeRestrictedCryptography();
        }

        if(disableHostnameVerification)
            SSLUtils.disableSSLHostnameVerification();

        TrustManager[] trustManagers;
        if(disableCertificateChecking
           || "true".equalsIgnoreCase(System.getProperty("disable.ssl.cert.checks")))
        {
            trustManagers = SSLUtils.getTrustAllCertsTrustManagers();
        }
        else if(null != trustStoreFilename)
        {
            if(null == trustStoreType)
                trustStoreType = "JKS";

            trustManagers = SSLUtils.getTrustManagers(trustStoreFilename, trustStorePassword, trustStoreType, trustStoreProvider, trustStoreAlgorithm, null, crlFilename);
        }
        else
            trustManagers = null;

        int port = 443;
        String host = args[argIndex];

        int pos = host.indexOf(':');
        if(pos > 0)
        {
            port = Integer.parseInt(host.substring(pos + 1));
            host = host.substring(0, pos);
        }

        // Enable all algorithms
        Security.setProperty("jdk.tls.disabledAlgorithms", "");

        List<String> supportedProtocols;

        if(null == sslEnabledProtocols)
        {
            // Auto-detect protocols
            ArrayList<String> protocols = new ArrayList<String>();
            // TODO: Allow the specification of a specific provider (or set?)
            for(Provider provider : Security.getProviders())
            {
                for(Object prop : provider.keySet())
                {
                    String key = (String)prop;
                    if(key.startsWith("SSLContext.")
                       && !key.equals("SSLContext.Default")
                       && key.matches(".*[0-9].*"))
                        protocols.add(key.substring("SSLContext.".length()));
                    else if(key.startsWith("Alg.Alias.SSLContext.")
                            && key.matches(".*[0-9].*"))
                        protocols.add(key.substring("Alg.Alias.SSLContext.".length()));
                }
            }
            Collections.sort(protocols); // Should give us a nice sort-order by default
            System.err.println("Auto-detected client-supported protocols: " + protocols);
            supportedProtocols = protocols;
            sslEnabledProtocols = supportedProtocols.toArray(new String[supportedProtocols.size()]);
        }
        else
        {
            supportedProtocols = new ArrayList<String>(Arrays.asList(sslEnabledProtocols));
        }

        System.out.println("Testing server " + host + ":" + port);

        SecureRandom rand = new SecureRandom();

        String reportFormat = "%9s %8s %s\n";
        System.out.print(String.format(reportFormat, "Supported", "Protocol", "Cipher"));

        InetSocketAddress address = new InetSocketAddress(host, port);

        for(String protocol : sslEnabledProtocols)
        {
            SSLContext sc;
            try
            {
                sc = SSLContext.getInstance(protocol);
            }
            catch (NoSuchAlgorithmException nsae)
            {
                System.out.print(String.format(reportFormat, "-----", protocol, " Not supported by client"));
                supportedProtocols.remove(protocol);
                continue;
            }
            catch (Exception e)
            {
                e.printStackTrace();
                continue; // Skip this protocol
            }

            sc.init(null, null, rand);

            // Restrict cipher suites to those specified by sslCipherSuites
            HashSet<String> cipherSuites = new HashSet<String>();
            cipherSuites.addAll(Arrays.asList(sc.getSocketFactory().getSupportedCipherSuites()));
            if(null != sslCipherSuites)
                cipherSuites.retainAll(Arrays.asList(sslCipherSuites));

            if(cipherSuites.isEmpty())
            {
                System.err.println("No overlapping cipher suites found for protocol " + protocol);
                supportedProtocols.remove(protocol);
                continue; // Go to the next protocol
            }

            for(String cipherSuite : cipherSuites)
            {
                String status;

                SSLSocketFactory sf = SSLUtils.getSSLSocketFactory(protocol,
                                                                   new String[] { protocol },
                                                                   new String[] { cipherSuite },
                                                                   rand,
                                                                   trustManagers);

                Socket sock = null;

                try
                {
                    //
                    // Note: SSLSocketFactory has several create() methods.
                    // Those that take arguments all connect immediately
                    // and have no options for specifying a connection timeout.
                    //
                    // So, we have to create a socket and connect it (with a
                    // connection timeout), then have the SSLSocketFactory wrap
                    // the already-connected socket.
                    //
                    sock = new Socket();
                    sock.setSoTimeout(readTimeout);
                    sock.connect(address, connectTimeout);

                    // Wrap plain socket in an SSL socket
                    SSLSocket socket = (SSLSocket)sf.createSocket(sock, host, port, true);
                    socket.startHandshake();

                    assert protocol.equals(socket.getSession().getProtocol());
                    assert cipherSuite.equals(socket.getSession().getCipherSuite());

                    status = "Accepted";
                }
                catch (SocketTimeoutException ste)
                {
                    status = "Failed";
                }
                catch (IOException ioe)
                {
                    // System.out.println(ioe);
                    status = "Rejected";
                }
                catch (Exception e)
                {
                    System.out.print(e.getMessage());
                    status = "Rejected";
                }
                finally
                {
                    if(null != sock) try { sock.close(); }
                    catch (IOException ioe) { ioe.printStackTrace(); }
                }
                System.out.print(String.format(reportFormat,
                                               status,
                                               protocol,
                                               cipherSuite));

            }
        }

        if(supportedProtocols.isEmpty())
        {
            System.err.println("No protocols ");
        }
        // Now get generic and allow the server to decide on the protocol and cipher suite
        String[] protocolsToTry = supportedProtocols.toArray(new String[supportedProtocols.size()]);

        SSLSocketFactory sf = SSLUtils.getSSLSocketFactory(sslProtocol,
                                                           protocolsToTry,
                                                           sslCipherSuites,
                                                           rand,
                                                           trustManagers);

        System.out.println("Finally try with those supported confs:");
        System.out.println("\tsslProtocol=" + sslProtocol);
        System.out.println("\tprotocols  =" + Arrays.toString(protocolsToTry));
        System.out.println("\tciphers    =" + Arrays.toString(sslCipherSuites));
        Socket sock = null;

        try
        {
            //
            // Note: SSLSocketFactory has several create() methods.
            // Those that take arguments all connect immediately
            // and have no options for specifying a connection timeout.
            //
            // So, we have to create a socket and connect it (with a
            // connection timeout), then have the SSLSocketFactory wrap
            // the already-connected socket.
            //
            sock = new Socket();
            sock.connect(address, connectTimeout);
            sock.setSoTimeout(readTimeout);

            // Wrap plain socket in an SSL socket
            SSLSocket socket = (SSLSocket)sf.createSocket(sock, host, port, true);
            socket.startHandshake();

            System.out.print("Given this client's capabilities ("
                             + supportedProtocols
                             + "), the server prefers protocol=");
            System.out.print(socket.getSession().getProtocol());
            System.out.print(", cipher=");
            System.out.println(socket.getSession().getCipherSuite());

            if(showCerts)
            {
                for(Certificate cert : socket.getSession().getPeerCertificates())
                {
                    System.out.println("Certificate: " + cert.getType());
                    if("X.509".equals(cert.getType()))
                    {
                        X509Certificate x509 = (X509Certificate)cert;
                        System.out.println("Subject: " + x509.getSubjectDN());
                        System.out.println("Issuer: " + x509.getIssuerDN());
                        System.out.println("Serial: " + x509.getSerialNumber());
//                        System.out.println("Signature: " + toHexString(x509.getSignature()));
//                        System.out.println("cert bytes: " + toHexString(cert.getEncoded()));
//                        System.out.println("cert bytes: " + cert.getPublicKey());
                    }
                    else
                    {
                        System.out.println("Unknown certificate type (" + cert.getType() + "): " + cert);
                    }
                }
            }
        }
        finally
        {
            if (null != sock) try { sock.close(); }
            catch (IOException ioe) { ioe.printStackTrace(); }
        }
    }
    static final char[] hexChars = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'a', 'b', 'c', 'd', 'e', 'f' };
    static String toHexString(byte[] bytes)
    {
        StringBuilder sb = new StringBuilder(bytes.length * 2);

        for(byte b : bytes)
            sb.append(hexChars[(b >> 4) & 0x0f])
              .append(hexChars[b & 0x0f]);

        return sb.toString();
    }
}