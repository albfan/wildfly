/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.UnaryCapabilityNameResolver;
import org.jboss.as.clustering.controller.ResourceCapabilityReference;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.UnaryRequirementCapability;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jgroups.JChannel;
import org.wildfly.clustering.jgroups.spi.JGroupsDefaultRequirement;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.service.UnaryRequirement;

/**
 * Resource description for the addressable resource and its alias
 *
 * /subsystem=infinispan/cache-container=X/transport=jgroups
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class JGroupsTransportResourceDefinition extends TransportResourceDefinition {

    static final PathElement PATH = pathElement("jgroups");

    enum Requirement implements UnaryRequirement {
        CHANNEL("org.wildfly.clustering.infinispan.transport.channel", JChannel.class),
        ;
        private final String name;
        private final Class<?> type;

        Requirement(String name, Class<?> type) {
            this.name = name;
            this.type = type;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public Class<?> getType() {
            return this.type;
        }
    }

    enum Capability implements org.jboss.as.clustering.controller.Capability {
        TRANSPORT_CHANNEL(Requirement.CHANNEL),
        ;
        private final RuntimeCapability<Void> definition;

        Capability(UnaryRequirement requirement) {
            this.definition = new UnaryRequirementCapability(requirement, UnaryCapabilityNameResolver.PARENT).getDefinition();
        }

        @Override
        public RuntimeCapability<Void> getDefinition() {
            return this.definition;
        }
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        CHANNEL("channel", ModelType.STRING, null) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(false)
                        .setCapabilityReference(new CapabilityReference(Capability.TRANSPORT_CHANNEL, JGroupsRequirement.CHANNEL_FACTORY))
                        ;
            }
        },
        LOCK_TIMEOUT("lock-timeout", ModelType.LONG, new ModelNode(TimeUnit.MINUTES.toMillis(4))) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setMeasurementUnit(MeasurementUnit.MILLISECONDS);
            }
        },
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES))
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }

        @Override
        public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
            return builder;
        }
    }

    static class ResourceDescriptorConfigurator implements UnaryOperator<ResourceDescriptor> {
        @Override
        public ResourceDescriptor apply(ResourceDescriptor descriptor) {
            return descriptor.addAttributes(Attribute.class)
                    .addCapabilities(Capability.class)
                    .addResourceCapabilityReference(new ResourceCapabilityReference(Capability.TRANSPORT_CHANNEL, JGroupsDefaultRequirement.CHANNEL_FACTORY))
                    ;
        }
    }

    JGroupsTransportResourceDefinition() {
        super(PATH, new ResourceDescriptorConfigurator(), new JGroupsTransportServiceHandler());
    }
}
