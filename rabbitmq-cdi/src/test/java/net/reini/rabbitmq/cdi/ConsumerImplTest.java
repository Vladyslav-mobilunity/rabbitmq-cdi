/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015, 2019 Patrick Reinhart
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package net.reini.rabbitmq.cdi;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;

/**
 * Tests the {@link ConsumerImpl} implementation.
 *
 * @author Patrick Reinhart
 */
@SuppressWarnings("boxing")
@ExtendWith(MockitoExtension.class)
public class ConsumerImplTest {
  @Mock
  private EnvelopeConsumer envelopeConsumer;
  @Mock
  private Channel channel;

  private Consumer consumer;
  private Consumer consumerAcknowledged;

  @BeforeEach
  public void setUp() {
    consumer = ConsumerImpl.create(envelopeConsumer);
    consumerAcknowledged = ConsumerImpl.createAcknowledged(envelopeConsumer, channel);
  }

  /**
   * Test method for {@link ConsumerImpl#handleConsumeOk(String)}.
   */
  @Test
  public void testHandleConsumeOk() {
    consumer.handleConsumeOk("consumerTag");
    consumerAcknowledged.handleCancelOk("consumerTag");
  }

  /**
   * Test method for {@link ConsumerImpl#handleCancelOk(String)}.
   */
  @Test
  public void testHandleCancelOk() {
    consumer.handleCancelOk("consumerTag");
    consumerAcknowledged.handleCancelOk("consumerTag");
  }

  /**
   * Test method for {@link ConsumerImpl#handleCancel(String)}.
   * 
   * @throws IOException
   */
  @Test
  public void testHandleCancel() throws IOException {
    consumer.handleCancel("consumerTag");
    consumerAcknowledged.handleCancel("consumerTag");
  }

  /**
   * Test method for {@link ConsumerImpl#handleShutdownSignal(String, ShutdownSignalException)}.
   */
  @Test
  public void testHandleShutdownSignal() {
    ShutdownSignalException sig = new ShutdownSignalException(false, false, null, null);

    consumer.handleShutdownSignal("consumerTag", sig);
    consumerAcknowledged.handleShutdownSignal("consumerTag", sig);
  }

  /**
   * Test method for {@link ConsumerImpl#handleRecoverOk(String)}.
   */
  @Test
  public void testHandleRecoverOk() {
    consumer.handleRecoverOk("consumerTag");
    consumerAcknowledged.handleRecoverOk("consumerTag");
  }

  /**
   * Test method for {@link ConsumerImpl#handleDelivery(String, Envelope, BasicProperties, byte[])}.
   * 
   * @throws IOException
   */
  @Test
  public void testHandleDelivery() throws IOException {
    Envelope envelope = new Envelope(1234L, false, "exchange", "routingKey");
    BasicProperties basicProperties = new BasicProperties();
    byte[] bodyData = "the body data".getBytes();

    consumer.handleDelivery("consumerTag", envelope, basicProperties, bodyData);

    verify(envelopeConsumer).consume("consumerTag", envelope, basicProperties, bodyData);
  }

  /**
   * Test method for {@link ConsumerImpl#handleDelivery(String, Envelope, BasicProperties, byte[])}.
   * 
   * @throws IOException
   */
  @Test
  public void testHandleDelivery_acknowledged() throws IOException {
    Envelope envelope = new Envelope(1234L, false, "exchange", "routingKey");
    BasicProperties basicProperties = new BasicProperties();
    byte[] bodyData = "the body data".getBytes();

    when(envelopeConsumer.consume("consumerTag", envelope, basicProperties, bodyData))
        .thenReturn(true);

    consumerAcknowledged.handleDelivery("consumerTag", envelope, basicProperties, bodyData);

    verify(channel).basicAck(1234L, false);
  }

  /**
   * Test method for {@link ConsumerImpl#handleDelivery(String, Envelope, BasicProperties, byte[])}.
   * 
   * @throws IOException
   */
  @Test
  public void testHandleDelivery_not_acknowledged() throws IOException {
    Envelope envelope = new Envelope(1234L, false, "exchange", "routingKey");
    BasicProperties basicProperties = new BasicProperties();
    byte[] bodyData = "the body data".getBytes();

    when(envelopeConsumer.consume("consumerTag", envelope, basicProperties, bodyData))
        .thenReturn(false);

    consumerAcknowledged.handleDelivery("consumerTag", envelope, basicProperties, bodyData);

    verify(channel).basicNack(1234L, false, false);
  }

  /**
   * Test method for {@link ConsumerImpl#handleDelivery(String, Envelope, BasicProperties, byte[])}.
   * 
   * @throws IOException
   */
  @Test
  public void testHandleDelivery_not_acknowledged_with_requeue() throws IOException {
    Envelope envelope = new Envelope(1234L, false, "exchange", "routingKey");
    BasicProperties basicProperties = new BasicProperties();
    byte[] bodyData = "the body data".getBytes();

    when(envelopeConsumer.consume("consumerTag", envelope, basicProperties, bodyData))
        .thenThrow(new IOException());

    consumerAcknowledged.handleDelivery("consumerTag", envelope, basicProperties, bodyData);

    verify(channel).basicNack(1234L, false, true);
  }
}
