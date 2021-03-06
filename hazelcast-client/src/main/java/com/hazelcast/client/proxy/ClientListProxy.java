/*
 * Copyright (c) 2008-2016, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.client.proxy;

import com.hazelcast.client.impl.protocol.ClientMessage;
import com.hazelcast.client.impl.protocol.codec.ListAddAllCodec;
import com.hazelcast.client.impl.protocol.codec.ListAddAllWithIndexCodec;
import com.hazelcast.client.impl.protocol.codec.ListAddCodec;
import com.hazelcast.client.impl.protocol.codec.ListAddListenerCodec;
import com.hazelcast.client.impl.protocol.codec.ListAddWithIndexCodec;
import com.hazelcast.client.impl.protocol.codec.ListClearCodec;
import com.hazelcast.client.impl.protocol.codec.ListCompareAndRemoveAllCodec;
import com.hazelcast.client.impl.protocol.codec.ListCompareAndRetainAllCodec;
import com.hazelcast.client.impl.protocol.codec.ListContainsAllCodec;
import com.hazelcast.client.impl.protocol.codec.ListContainsCodec;
import com.hazelcast.client.impl.protocol.codec.ListGetAllCodec;
import com.hazelcast.client.impl.protocol.codec.ListGetCodec;
import com.hazelcast.client.impl.protocol.codec.ListIndexOfCodec;
import com.hazelcast.client.impl.protocol.codec.ListIsEmptyCodec;
import com.hazelcast.client.impl.protocol.codec.ListIteratorCodec;
import com.hazelcast.client.impl.protocol.codec.ListLastIndexOfCodec;
import com.hazelcast.client.impl.protocol.codec.ListListIteratorCodec;
import com.hazelcast.client.impl.protocol.codec.ListRemoveCodec;
import com.hazelcast.client.impl.protocol.codec.ListRemoveListenerCodec;
import com.hazelcast.client.impl.protocol.codec.ListRemoveWithIndexCodec;
import com.hazelcast.client.impl.protocol.codec.ListSetCodec;
import com.hazelcast.client.impl.protocol.codec.ListSizeCodec;
import com.hazelcast.client.impl.protocol.codec.ListSubCodec;
import com.hazelcast.client.spi.ClientClusterService;
import com.hazelcast.client.spi.EventHandler;
import com.hazelcast.client.spi.impl.ListenerMessageCodec;
import com.hazelcast.collection.impl.common.DataAwareItemEvent;
import com.hazelcast.core.IList;
import com.hazelcast.core.ItemEvent;
import com.hazelcast.core.ItemEventType;
import com.hazelcast.core.ItemListener;
import com.hazelcast.core.Member;
import com.hazelcast.internal.serialization.SerializationService;
import com.hazelcast.nio.serialization.Data;
import com.hazelcast.spi.impl.UnmodifiableLazyList;
import com.hazelcast.util.CollectionUtil;
import com.hazelcast.util.Preconditions;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Proxy implementation of {@link IList}.
 *
 * @param <E> the type of elements in this list
 */
public class ClientListProxy<E> extends PartitionSpecificClientProxy implements IList<E> {

    public ClientListProxy(String serviceName, String name) {
        super(serviceName, name);
    }

    public boolean addAll(int index, Collection<? extends E> c) {
        Preconditions.checkNotNull(c);
        Collection<Data> dataCollection = CollectionUtil.objectToDataCollection(c, getSerializationService());
        ClientMessage request = ListAddAllWithIndexCodec.encodeRequest(name, index, dataCollection);
        ClientMessage response = invokeOnPartition(request);
        ListAddAllWithIndexCodec.ResponseParameters resultParameters =
                ListAddAllWithIndexCodec.decodeResponse(response);
        return resultParameters.response;
    }

    public E get(int index) {
        ClientMessage request = ListGetCodec.encodeRequest(name, index);
        ClientMessage response = invokeOnPartition(request);
        ListGetCodec.ResponseParameters resultParameters = ListGetCodec.decodeResponse(response);
        return toObject(resultParameters.response);
    }

    public E set(int index, E element) {
        Preconditions.checkNotNull(element);
        Data value = toData(element);
        ClientMessage request = ListSetCodec.encodeRequest(name, index, value);
        ClientMessage response = invokeOnPartition(request);
        ListSetCodec.ResponseParameters resultParameters = ListSetCodec.decodeResponse(response);
        return toObject(resultParameters.response);
    }

    public void add(int index, E element) {
        Preconditions.checkNotNull(element);
        Data value = toData(element);
        ClientMessage request = ListAddWithIndexCodec.encodeRequest(name, index, value);
        invokeOnPartition(request);
    }

    public E remove(int index) {
        ClientMessage request = ListRemoveWithIndexCodec.encodeRequest(name, index);
        ClientMessage response = invokeOnPartition(request);
        ListRemoveWithIndexCodec.ResponseParameters resultParameters = ListRemoveWithIndexCodec.decodeResponse(response);
        return toObject(resultParameters.response);
    }

    public int size() {
        ClientMessage request = ListSizeCodec.encodeRequest(name);
        ClientMessage response = invokeOnPartition(request);
        ListSizeCodec.ResponseParameters resultParameters = ListSizeCodec.decodeResponse(response);
        return resultParameters.response;
    }

    public boolean isEmpty() {
        ClientMessage request = ListIsEmptyCodec.encodeRequest(name);
        ClientMessage response = invokeOnPartition(request);
        ListIsEmptyCodec.ResponseParameters resultParameters = ListIsEmptyCodec.decodeResponse(response);
        return resultParameters.response;
    }

    public boolean contains(Object o) {
        Preconditions.checkNotNull(o);
        Data value = toData(o);
        ClientMessage request = ListContainsCodec.encodeRequest(name, value);
        ClientMessage response = invokeOnPartition(request);
        ListContainsCodec.ResponseParameters resultParameters = ListContainsCodec.decodeResponse(response);
        return resultParameters.response;
    }

    public Iterator<E> iterator() {
        ClientMessage request = ListIteratorCodec.encodeRequest(name);
        ClientMessage response = invokeOnPartition(request);
        ListIteratorCodec.ResponseParameters resultParameters = ListIteratorCodec.decodeResponse(response);
        List<Data> resultCollection = (List<Data>) resultParameters.response;
        SerializationService serializationService = getContext().getSerializationService();
        return new UnmodifiableLazyList<E>(resultCollection, serializationService).iterator();
    }

    public Object[] toArray() {
        return getAll().toArray();
    }

    public <T> T[] toArray(T[] a) {
        return getAll().toArray(a);
    }

    public boolean add(E e) {
        Preconditions.checkNotNull(e);
        Data element = toData(e);
        ClientMessage request = ListAddCodec.encodeRequest(name, element);
        ClientMessage response = invokeOnPartition(request);
        ListAddCodec.ResponseParameters resultParameters = ListAddCodec.decodeResponse(response);
        return resultParameters.response;
    }

    public boolean remove(Object o) {
        Preconditions.checkNotNull(o);
        Data value = toData(o);
        ClientMessage request = ListRemoveCodec.encodeRequest(name, value);
        ClientMessage response = invokeOnPartition(request);
        ListRemoveCodec.ResponseParameters resultParameters = ListRemoveCodec.decodeResponse(response);
        return resultParameters.response;
    }

    public boolean containsAll(Collection<?> c) {
        Preconditions.checkNotNull(c);
        Collection<Data> dataCollection = CollectionUtil.objectToDataCollection(c, getSerializationService());
        ClientMessage request = ListContainsAllCodec.encodeRequest(name, dataCollection);
        ClientMessage response = invokeOnPartition(request);
        ListContainsAllCodec.ResponseParameters resultParameters = ListContainsAllCodec.decodeResponse(response);
        return resultParameters.response;
    }

    public boolean addAll(Collection<? extends E> c) {
        Preconditions.checkNotNull(c);
        Collection<Data> dataCollection = CollectionUtil.objectToDataCollection(c, getSerializationService());
        ClientMessage request = ListAddAllCodec.encodeRequest(name, dataCollection);
        ClientMessage response = invokeOnPartition(request);
        ListAddAllCodec.ResponseParameters resultParameters = ListAddAllCodec.decodeResponse(response);
        return resultParameters.response;
    }

    public boolean removeAll(Collection<?> c) {
        Preconditions.checkNotNull(c);
        Collection<Data> dataCollection = CollectionUtil.objectToDataCollection(c, getSerializationService());
        ClientMessage request = ListCompareAndRemoveAllCodec.encodeRequest(name, dataCollection);
        ClientMessage response = invokeOnPartition(request);
        ListCompareAndRemoveAllCodec.ResponseParameters resultParameters =
                ListCompareAndRemoveAllCodec.decodeResponse(response);
        return resultParameters.response;
    }

    public boolean retainAll(Collection<?> c) {
        Preconditions.checkNotNull(c);
        Collection<Data> dataCollection = CollectionUtil.objectToDataCollection(c, getSerializationService());
        ClientMessage request = ListCompareAndRetainAllCodec.encodeRequest(name, dataCollection);
        ClientMessage response = invokeOnPartition(request);
        ListCompareAndRetainAllCodec.ResponseParameters resultParameters =
                ListCompareAndRetainAllCodec.decodeResponse(response);
        return resultParameters.response;
    }

    public void clear() {
        ClientMessage request = ListClearCodec.encodeRequest(name);
        invokeOnPartition(request);
    }

    @Override
    public String addItemListener(ItemListener<E> listener, boolean includeValue) {
        EventHandler<ClientMessage> eventHandler = new ItemEventHandler(listener);
        return registerListener(createItemListenerCodec(includeValue), eventHandler);
    }

    private ListenerMessageCodec createItemListenerCodec(final boolean includeValue) {
        return new ListenerMessageCodec() {
            @Override
            public ClientMessage encodeAddRequest(boolean localOnly) {
                return ListAddListenerCodec.encodeRequest(name, includeValue, localOnly);
            }

            @Override
            public String decodeAddResponse(ClientMessage clientMessage) {
                return ListAddListenerCodec.decodeResponse(clientMessage).response;
            }

            @Override
            public ClientMessage encodeRemoveRequest(String realRegistrationId) {
                return ListRemoveListenerCodec.encodeRequest(name, realRegistrationId);
            }

            @Override
            public boolean decodeRemoveResponse(ClientMessage clientMessage) {
                return ListRemoveListenerCodec.decodeResponse(clientMessage).response;
            }
        };
    }

    public boolean removeItemListener(String registrationId) {
        return deregisterListener(registrationId);
    }

    private Collection<E> getAll() {
        ClientMessage request = ListGetAllCodec.encodeRequest(name);
        ClientMessage response = invokeOnPartition(request);
        ListGetAllCodec.ResponseParameters resultParameters = ListGetAllCodec.decodeResponse(response);
        return new UnmodifiableLazyList<E>(resultParameters.response, getSerializationService());
    }

    public int lastIndexOf(Object o) {
        Preconditions.checkNotNull(o);
        Data value = toData(o);
        ClientMessage request = ListLastIndexOfCodec.encodeRequest(name, value);
        ClientMessage response = invokeOnPartition(request);
        ListLastIndexOfCodec.ResponseParameters resultParameters = ListLastIndexOfCodec.decodeResponse(response);
        return resultParameters.response;
    }

    public int indexOf(Object o) {
        Preconditions.checkNotNull(o);
        Data value = toData(o);
        ClientMessage request = ListIndexOfCodec.encodeRequest(name, value);
        ClientMessage response = invokeOnPartition(request);
        ListIndexOfCodec.ResponseParameters resultParameters = ListIndexOfCodec.decodeResponse(response);
        return resultParameters.response;
    }

    public ListIterator<E> listIterator() {
        return listIterator(0);
    }

    public ListIterator<E> listIterator(int index) {
        ClientMessage request = ListListIteratorCodec.encodeRequest(name, index);
        ClientMessage response = invokeOnPartition(request);
        ListListIteratorCodec.ResponseParameters resultParameters = ListListIteratorCodec.decodeResponse(response);
        List<Data> resultCollection = resultParameters.response;
        SerializationService serializationService = getContext().getSerializationService();
        return new UnmodifiableLazyList<E>(resultCollection, serializationService).listIterator();
    }

    public List<E> subList(int fromIndex, int toIndex) {
        ClientMessage request = ListSubCodec.encodeRequest(name, fromIndex, toIndex);
        ClientMessage response = invokeOnPartition(request);
        ListSubCodec.ResponseParameters resultParameters = ListSubCodec.decodeResponse(response);
        List<Data> resultCollection = resultParameters.response;
        SerializationService serializationService = getContext().getSerializationService();
        return new UnmodifiableLazyList<E>(resultCollection, serializationService);
    }

    @Override
    public String toString() {
        return "IList{" + "name='" + name + '\'' + '}';
    }

    private class ItemEventHandler extends ListAddListenerCodec.AbstractEventHandler
            implements EventHandler<ClientMessage> {

        private final ItemListener<E> listener;

        public ItemEventHandler(ItemListener<E> listener) {
            this.listener = listener;
        }

        @Override
        public void handle(Data dataItem, String uuid, int eventType) {
            SerializationService serializationService = getContext().getSerializationService();
            ClientClusterService clusterService = getContext().getClusterService();
            Member member = clusterService.getMember(uuid);
            ItemEvent<E> itemEvent = new DataAwareItemEvent(name, ItemEventType.getByType(eventType),
                    dataItem, member, serializationService);
            if (eventType == ItemEventType.ADDED.getType()) {
                listener.itemAdded(itemEvent);
            } else {
                listener.itemRemoved(itemEvent);
            }
        }

        @Override
        public void beforeListenerRegister() {

        }

        @Override
        public void onListenerRegister() {

        }
    }
}
