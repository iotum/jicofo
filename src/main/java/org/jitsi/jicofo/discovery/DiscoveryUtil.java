/*
 * Jicofo, the Jitsi Conference Focus.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jitsi.jicofo.discovery;

import org.jitsi.impl.protocol.xmpp.*;
import org.jitsi.utils.logging.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smackx.disco.*;
import org.jivesoftware.smackx.disco.packet.*;
import org.jxmpp.jid.*;

import java.util.*;
import java.util.stream.*;

/**
 * Utility class for feature discovery.
 *
 * @author Pawel Domas
 */
public class DiscoveryUtil
{
    /**
     * The logger
     */
    private final static Logger logger = Logger.getLogger(DiscoveryUtil.class);

    /**
     * List contains default feature set.
     */
    private static ArrayList<String> defaultFeatures;

    /**
     * Audio RTP feature name.
     */
    public final static String FEATURE_AUDIO
            = "urn:xmpp:jingle:apps:rtp:audio";

    /**
     * Video RTP feature name.
     */
    public final static String FEATURE_VIDEO
            = "urn:xmpp:jingle:apps:rtp:video";

    /**
     * ICE feature name.
     */
    public final static String FEATURE_ICE = "urn:xmpp:jingle:transports:ice-udp:1";

    /**
     * DTLS/SCTP feature name.
     */
    public final static String FEATURE_SCTP = "urn:xmpp:jingle:transports:dtls-sctp:1";

    /**
     * RTX (RFC4588) support.
     */
    public final static String FEATURE_RTX = "urn:ietf:rfc:4588";

    /**
     * The Jingle DTLS feature name (XEP-0320).
     */
    public final static String FEATURE_DTLS = "urn:xmpp:jingle:apps:dtls:0";

    /**
     * RTCP mux feature name.
     */
    public final static String FEATURE_RTCP_MUX = "urn:ietf:rfc:5761";

    /**
     * RTP bundle feature name.
     */
    public final static String FEATURE_RTP_BUNDLE = "urn:ietf:rfc:5888";

    public final static String FEATURE_OPUS_RED = "http://jitsi.org/opus-red";

    /**
     * A namespace for our custom "lip-sync" feature. Advertised by the clients
     * that support all of the functionality required for doing the lip-sync
     * properly.
     */
    public final static String FEATURE_LIPSYNC = "http://jitsi.org/meet/lipsync";

    /**
     * A namespace for detecting participants as jigasi users.
     */
    public final static String FEATURE_JIGASI = "http://jitsi.org/protocol/jigasi";

    /**
     * A namespace for detecting whether a participant (jigasi users) can be
     * muted.
     */
    public final static String FEATURE_AUDIO_MUTE = "http://jitsi.org/protocol/audio-mute";

    /**
     * Gets the list of features supported by participant. If we fail to
     * obtain it due to network failure default feature list is returned.
     * @param protocolProvider protocol provider service instance that will
     *        be used for discovery.
     * @param address XMPP address of the participant.
     */
    public static List<String> discoverParticipantFeatures(XmppProvider protocolProvider, EntityFullJid address)
    {
        XMPPConnection xmppConnection = protocolProvider.getXmppConnectionRaw();
        if (xmppConnection == null)
        {
            logger.error("No xmpp connection " + protocolProvider);
            return getDefaultParticipantFeatureSet();
        }
        ServiceDiscoveryManager discoveryManager = ServiceDiscoveryManager.getInstanceFor(xmppConnection);
        if (discoveryManager == null)
        {
            logger.error("Service discovery not supported by " + protocolProvider);
            return getDefaultParticipantFeatureSet();
        }

        long start = System.currentTimeMillis();

        logger.info("Doing feature discovery for " + address);

        List<String> participantFeatures = null;
        try
        {
            DiscoverInfo info = discoveryManager.discoverInfo(address);
            if (info != null)
            {
                participantFeatures = info.getFeatures()
                        .stream()
                        .map(DiscoverInfo.Feature::getVar)
                        .collect(Collectors.toList());
            }
        }
        catch (Exception e)
        {
            logger.warn(String.format( "Failed to discover features for %s: %s", address, e.getMessage()));
        }

        if (participantFeatures == null)
        {
            logger.warn("Failed to discover features for "+ address + " assuming default feature set.");

            return getDefaultParticipantFeatureSet();
        }

        long tookMillis = System.currentTimeMillis() - start;

        if (logger.isDebugEnabled())
        {
            StringBuilder sb
                = new StringBuilder(address)
                    .append(", features: ")
                    .append(String.join(", ", participantFeatures))
                    .append(", in: ")
                    .append(tookMillis);
            logger.debug(sb);
        }
        else
        {
            logger.info(
                String.format(
                    "Successfully discovered features for %s in %d",
                    address,
                    tookMillis));
        }

        return participantFeatures;
    }

    /**
     * Returns default participant feature set(all features).
     */
    static public List<String> getDefaultParticipantFeatureSet()
    {
        if (defaultFeatures == null)
        {
            defaultFeatures = new ArrayList<>();
            defaultFeatures.add(FEATURE_AUDIO);
            defaultFeatures.add(FEATURE_VIDEO);
            defaultFeatures.add(FEATURE_ICE);
            defaultFeatures.add(FEATURE_SCTP);
            defaultFeatures.add(FEATURE_DTLS);
            defaultFeatures.add(FEATURE_RTCP_MUX);
            defaultFeatures.add(FEATURE_RTP_BUNDLE);
        }
        return defaultFeatures;
    }

    /**
     * Returns <tt>true</tt> if <tt>list1</tt> and <tt>list2</tt> contain the
     * same elements where items order is not relevant.
     * @param list1 the first list of <tt>String</tt> to be compared against
     *              the second list.
     * @param list2 the second list of <tt>String</tt> to be compared against
     *              the first list.
     */
    static public boolean areTheSame(List<String> list1, List<String> list2)
    {
        return list1.size() == list2.size()
            && list2.containsAll(list1)
            && list1.containsAll(list2);
    }
}
