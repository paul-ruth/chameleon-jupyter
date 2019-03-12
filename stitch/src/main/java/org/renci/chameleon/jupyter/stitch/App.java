package org.renci.chameleon.jupyter.stitch;

import org.renci.ahab.libndl.Slice;
import org.renci.ahab.libndl.resources.request.ComputeNode;
import org.renci.ahab.libndl.resources.request.InterfaceNode2Net;
import org.renci.ahab.libndl.resources.request.Network;
import org.renci.ahab.libndl.resources.request.StitchPort;


import org.renci.ahab.libtransport.ISliceTransportAPIv1;
import org.renci.ahab.libtransport.ITransportProxyFactory;
import org.renci.ahab.libtransport.PEMTransportContext;
import org.renci.ahab.libtransport.SSHAccessToken;
import org.renci.ahab.libtransport.SliceAccessContext;
import org.renci.ahab.libtransport.TransportContext;
import org.renci.ahab.libtransport.util.SSHAccessTokenFileFactory;
import org.renci.ahab.libtransport.xmlrpc.XMLRPCProxyFactory;

import java.net.URL;

/**
 * `mvn clean package`
 * `java -cp ./target/stitch-1.0-SNAPSHOT-jar-with-dependencies.jar org.renci.chameleon.jupyter.stitch.App certLocation keyLocation controllerURL sliceName`
 * Verify slice creation in Flukes
 */
public class App
{
    public static void main( String[] args ) throws Exception
    {
        System.out.println( "Creating a simple Slice!" );

        // Need some command line options -- use your GENI cert
        // http://www.exogeni.net/2015/09/exogeni-getting-started-tutorial/
        String certLocation = args[0];
        String keyLocation = args[1];
        String controllerUrl = args[2]; // "https://geni.renci.org:11443/orca/xmlrpc";
        String sliceName = args[3]; // "your_name";

        /*
         * Get Slice Proxy
         */
        ISliceTransportAPIv1 sliceProxy;

        //ExoGENI controller context
        ITransportProxyFactory ifac = new XMLRPCProxyFactory();
        System.out.println("Opening certificate " + certLocation + " and key " + keyLocation);
        char [] pwd = System.console().readPassword("Enter password for key: ");
        TransportContext ctx = new PEMTransportContext(String.valueOf(pwd), certLocation, keyLocation);
        sliceProxy = ifac.getSliceProxy(ctx, new URL(controllerUrl));

        /*
         * SSH Context
         */
        SliceAccessContext<SSHAccessToken> sctx = new SliceAccessContext<>();

        SSHAccessTokenFileFactory fac;
        fac = new SSHAccessTokenFileFactory("~/.ssh/id_rsa.pub", false);

        SSHAccessToken t = fac.getPopulatedToken();
        sctx.addToken("root", "root", t);
        sctx.addToken("root", t);

        /*
         * Main Example Code
         */
        Slice slice = Slice.create(sliceProxy, sctx, sliceName);

        String nodeImageShortName = "Centos7";
        String nodeImageURL = "http://geni-images.renci.org/images/standard/centos/centos7.4-v1.0.3/centos7.4-v1.0.3.xml";
        String nodeImageHash = "ebab47f2f1e9b7702d200cfa384ad86048bd29cd";
        String nodeNodeType = "XO Medium";
        String nodePostBootScript = "#node boot script";
        String nodeDomain = "UFL (Gainesville, FL USA) XO Rack";

        String newNodeName = sliceName + "0";
        ComputeNode node0 = slice.addComputeNode(newNodeName);
        node0.setImage(nodeImageURL,nodeImageHash,nodeImageShortName);
        node0.setNodeType(nodeNodeType);
        node0.setDomain(nodeDomain);
        node0.setPostBootScript(nodePostBootScript);


        //StitchPort sp1 = slice.addStitchPort("sp1","3297","http://geni-orca.renci.org/owl/ion.rdf#AL2S/Chameleon/Cisco/6509/GigabitEthernet/1/1",10000000);
        StitchPort sp2 = slice.addStitchPort("sp2","3506","http://geni-orca.renci.org/owl/ion.rdf#AL2S/TACC/Cisco/6509/TenGigabitEthernet/1/1",10000000);

        //sp1.stitch(ap2);

        InterfaceNode2Net ifaceNode0 = (InterfaceNode2Net) sp2.stitch(node0);
        ifaceNode0.setIpAddress("192.168.1.10");
        ifaceNode0.setMacAddress("255.255.255.0");

        System.out.println(slice.getRequest());
        slice.commit();

/*
        while (true){

            slice.refresh();

            System.out.println("");
            System.out.println("Slice: " + slice.getAllResources());
            net1 = (Network) slice.getResourceByName(netName);
            System.out.println("Network State: "  + net1.getState());

            if(net1.getState() == "Active") break;

            try {
                Thread.sleep(10 * 1000);
            } catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
*/
    }
}
