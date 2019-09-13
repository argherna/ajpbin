package com.github.argherna.ajpbin;

import static com.github.argherna.ajpbin.Constants.PROTOCOL;
import static com.github.argherna.ajpbin.Constants.STATUS_CODES_DESCRIPTIONS;
import static com.github.argherna.ajpbin.Constants.WEBDAV_DEFAULT_LOCK_DURATION;
import static com.github.argherna.ajpbin.Constants.WEBDAV_DEFAULT_LOCK_OWNER;
import static com.github.argherna.ajpbin.Constants.WEBDAV_OPAQUE_LOCK_TOKEN;
import static com.github.argherna.ajpbin.Xml.EL_ACTIVELOCK;
import static com.github.argherna.ajpbin.Xml.EL_HREF;
import static com.github.argherna.ajpbin.Xml.EL_LOCKDISCOVERY;
import static com.github.argherna.ajpbin.Xml.EL_LOCKSCOPE;
import static com.github.argherna.ajpbin.Xml.EL_LOCKTOKEN;
import static com.github.argherna.ajpbin.Xml.EL_LOCKTYPE;
import static com.github.argherna.ajpbin.Xml.EL_MULTISTATUS;
import static com.github.argherna.ajpbin.Xml.EL_OWNER;
import static com.github.argherna.ajpbin.Xml.EL_PROP;
import static com.github.argherna.ajpbin.Xml.EL_RESPONSE;
import static com.github.argherna.ajpbin.Xml.EL_STATUS;
import static com.github.argherna.ajpbin.Xml.EL_TIMEOUT;
import static com.github.argherna.ajpbin.Xml.EL_WRITE;
import static com.github.argherna.ajpbin.Xml.NS_DAV;
import static com.github.argherna.ajpbin.Xml.NS_DAV_PREFIX;
import static com.github.argherna.ajpbin.Xml.newNamespaceDocument;
import static com.github.argherna.ajpbin.Xml.newPrefixedNamespacedElement;
import static java.util.Collections.EMPTY_SET;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;

final class DocumentBuilders {
    private DocumentBuilders() {
    }

    static final MultistatusDocumentBuilder multistatusDocumentBuilder(
            Collection<Map.Entry<Integer, String>> possibleStatusValues) {
        return new MultistatusDocumentBuilder(possibleStatusValues);
    }

    static final LockDocumentBuilder lockDocumentBuilder() {
        return new LockDocumentBuilder();
    }

    static final class MultistatusDocumentBuilder {

        private final Collection<Map.Entry<Integer, String>> possibleStatusValues;

        private Collection<Map.Entry<Integer, String>> requestedStatusValues = new ArrayList<>();

        @SuppressWarnings("unchecked")
        MultistatusDocumentBuilder() {
            this(EMPTY_SET);
        }

        MultistatusDocumentBuilder(Collection<Map.Entry<Integer, String>> possibleStatusValues) {
            this.possibleStatusValues = possibleStatusValues;
        }

        MultistatusDocumentBuilder addRequestedStatusValue(Map.Entry<Integer, String> requestedStatusValue) {
            requestedStatusValues.add(requestedStatusValue);
            return this;
        }

        Document build() throws ParserConfigurationException {
            var doc = newNamespaceDocument();
            var multistatus = newPrefixedNamespacedElement(doc, NS_DAV, EL_MULTISTATUS, NS_DAV_PREFIX);
            for (Map.Entry<Integer, String> statusValue : possibleStatusValues) {
                var href = newPrefixedNamespacedElement(doc, NS_DAV, EL_HREF, NS_DAV_PREFIX);
                href.setTextContent(statusValue.getValue());

                var status = newPrefixedNamespacedElement(doc, NS_DAV, EL_STATUS, NS_DAV_PREFIX);
                status.setTextContent(multistatusLine(statusValue.getKey()));

                var resp = newPrefixedNamespacedElement(doc, NS_DAV, EL_RESPONSE, NS_DAV_PREFIX);
                resp.appendChild(href);
                resp.appendChild(status);
      
                multistatus.appendChild(resp);
            }
            return doc;
        }

        private String multistatusLine(int sc) {
            return new StringBuilder(PROTOCOL).append(" ").append(sc).append(" ")
                    .append(STATUS_CODES_DESCRIPTIONS.get(sc)).toString();
        }
    }

    static final class LockDocumentBuilder {

        private LockScope lockscope = LockScope.SHARED;

        private Duration timeout = WEBDAV_DEFAULT_LOCK_DURATION;

        private UUID opaqueLockToken = UUID.randomUUID();

        private String lockType = "";

        private String owner = WEBDAV_DEFAULT_LOCK_OWNER;

        LockDocumentBuilder lockscope(LockScope lockscope) {
            this.lockscope = lockscope;
            return this;
        }

        LockDocumentBuilder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        LockDocumentBuilder opaqueLockToken(UUID opaqueLockToken) {
            this.opaqueLockToken = opaqueLockToken;
            return this;
        }

        LockDocumentBuilder lockType(String lockType) {
            this.lockType = lockType;
            return this;
        }

        LockDocumentBuilder owner(String owner) {
            this.owner = owner;
            return this;
        }

        Document build() throws ParserConfigurationException {
            var doc = newNamespaceDocument();
            var activelockElement = newPrefixedNamespacedElement(doc, NS_DAV, EL_ACTIVELOCK, NS_DAV_PREFIX);

            // Yoda condition -- keeps NPE from happening so ¯\_(ツ)_/¯
            if ("write".equals(lockType)) {
                var locktypeElement = newPrefixedNamespacedElement(doc, NS_DAV, EL_LOCKTYPE, NS_DAV_PREFIX);
                locktypeElement.appendChild(newPrefixedNamespacedElement(doc, NS_DAV, EL_WRITE, NS_DAV_PREFIX));
                activelockElement.appendChild(locktypeElement);
            }

            var lockscopeElement = newPrefixedNamespacedElement(doc, NS_DAV, EL_LOCKSCOPE, NS_DAV_PREFIX);
            lockscopeElement
                    .appendChild(newPrefixedNamespacedElement(doc, NS_DAV, lockscope.toString(), NS_DAV_PREFIX));
            activelockElement.appendChild(lockscopeElement);

            var ownerHrefElement = newPrefixedNamespacedElement(doc, NS_DAV, EL_HREF, NS_DAV_PREFIX);
            ownerHrefElement.setTextContent(owner);

            var ownerElement = newPrefixedNamespacedElement(doc, NS_DAV, EL_OWNER, NS_DAV_PREFIX);
            ownerElement.appendChild(ownerHrefElement);
            activelockElement.appendChild(ownerElement);

            var timeoutElement = newPrefixedNamespacedElement(doc, NS_DAV, EL_TIMEOUT, NS_DAV_PREFIX);
            timeoutElement.setTextContent(String.format("Seconds-%d", timeout.toSeconds()));
            activelockElement.appendChild(timeoutElement);

            var opaqueLockTokenHrefElement = newPrefixedNamespacedElement(doc, NS_DAV, EL_HREF, NS_DAV_PREFIX);
            opaqueLockTokenHrefElement
                    .setTextContent(String.format("%s%s", WEBDAV_OPAQUE_LOCK_TOKEN, opaqueLockToken.toString()));

            var lockTokenElement = newPrefixedNamespacedElement(doc, NS_DAV, EL_LOCKTOKEN, NS_DAV_PREFIX);
            lockTokenElement.appendChild(opaqueLockTokenHrefElement);
            activelockElement.appendChild(lockTokenElement);

            var lockdiscoveryElement = newPrefixedNamespacedElement(doc, NS_DAV, EL_LOCKDISCOVERY, NS_DAV_PREFIX);
            lockdiscoveryElement.appendChild(activelockElement);

            var propElement = newPrefixedNamespacedElement(doc, NS_DAV, EL_PROP, NS_DAV_PREFIX);
            propElement.appendChild(lockdiscoveryElement);

            doc.appendChild(propElement);

            return doc;
        }
    }

    static enum LockScope {
        EXCLUSIVE, SHARED;

        public String toString() {
            return this.name().toLowerCase();
        }
    }
}
