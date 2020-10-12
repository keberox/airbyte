/*
 * MIT License
 *
 * Copyright (c) 2020 Airbyte
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.airbyte.persistentqueue;

import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A queue for which the dequeue ({@link Queue#poll}) method will block if the queue is empty but it
 * is possible more records will be written in the future. It will return null if it is empty and no
 * more records can be written to it. If the queue is not empty, it will return the next element in
 * the queue. This queue is autocloseable allowing the ability to terminate any longer running
 * connections or resources gracefully.
 *
 * @param <E>
 */
public abstract class AbstractCloseableInputQueue<E> extends AbstractQueue<E> implements CloseableInputQueue<E> {

  private static final long WAIT_FOR_NEW_ELEMENT_MILLIS = 1000L;

  protected final AtomicBoolean inputClosed = new AtomicBoolean(false);
  protected final AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * Enqueue implementation for the queue.
   *
   * @param element - element to enqueue.
   * @return true if enqueue was successful, otherwise false.
   */
  protected abstract boolean enqueueInternal(E element);

  /**
   * Dequeue implementation for the queue.
   *
   * @return returns dequeued element.
   */
  protected abstract E pollInternal();

  /**
   * Close any connections or resources.
   *
   * @throws Exception - can throw any exception (same as {@link AutoCloseable}).
   */
  protected abstract void closeInternal() throws Exception;

  /**
   * Adds an element to the queue.
   *
   * @param element - element to enqueue.
   * @return true if enqueue successful, otherwise false.
   * @throws IllegalStateException if invoked after {@link CloseableInputQueue#closeInput()}
   */
  @Override
  public boolean offer(E element) {
    Preconditions.checkState(!closed.get());
    Preconditions.checkState(!inputClosed.get());
    Preconditions.checkNotNull(element);
    return enqueueInternal(element);
  }

  // Blocking call to dequeue an element.
  // (comment is up here to avoid auto format scrambling it up.)
  // | hasValue | inputClosed | behavior |
  // ----------------------------------------
  // | true | false | return val |
  // | false | false | block until |
  // | true | true | return val |
  // | false | true | return null |

  /**
   * Blocking call to dequeue an element.
   *
   * @return a value from the queue or null if the queue is empty and will not receive anymore data.
   */
  @SuppressWarnings("BusyWait")
  @Override
  public E poll() {
    Preconditions.checkState(!closed.get());
    // if the queue is closed, always stop.
    while (!closed.get()) {
      final E dequeue = pollInternal();

      // if we find a value, always return it.
      if (dequeue != null) {
        return dequeue;
      }

      // if there is nothing in the queue and there will be no more entries written, end.
      if (inputClosed.get()) {
        return null;
      }

      // if empty but entries could still be written, sleep then try again.
      try {
        Thread.sleep(WAIT_FOR_NEW_ELEMENT_MILLIS);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }
    return null;
  }

  @Override
  public void closeInput() {
    inputClosed.set(true);
  }

  @SuppressWarnings("NullableProblems")
  @Override
  public Iterator<E> iterator() {
    Preconditions.checkState(!closed.get());

    return new AbstractIterator<>() {

      @Override
      protected E computeNext() {
        final E poll = poll();
        if (poll == null) {
          return endOfData();
        }
        return poll;
      }

    };
  }

  @Override
  public void close() throws Exception {
    closed.set(true);
    closeInternal();
  }

}
