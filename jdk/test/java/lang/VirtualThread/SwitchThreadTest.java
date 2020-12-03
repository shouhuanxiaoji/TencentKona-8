/*
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company. All rights reserved.
 * DO NOT ALTER OR REMOVE NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as
 * published by the Free Software Foundation. THL A29 Limited designates 
 * this particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License version 2 for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

/*
 * @test
 * @run testng/othervm -XX:+UnlockDiagnosticVMOptions -XX:+VerifyCoroutineStateOnYield SwitchThreadTest
 * @summary Basic test for continuation, test create/run/yield/resume/stop
 */

import static org.testng.Assert.*;
import org.testng.annotations.Test;

public class SwitchThreadTest {
    static long count = 0;
    static ContinuationScope scope = new ContinuationScope("test");
    @Test
    public static void test() throws Exception {
        foo();
        System.out.println("finish first");
        foo();
        System.out.println("finish second");
        bar();
        System.out.println("finish bar 1");
        qux();
        System.out.println("finish qux 1");
        switch3threads();
        System.out.println("finish switch3threads 1");
        baz();
        System.out.println("finish baz 1");
    }

    static void foo() throws Exception {
        final Thread kernel = Thread.currentThread();
        Runnable target = new Runnable() {
            public void run() {
                System.out.println("before yield, Continuation run in " + Thread.currentThread().getName());
                assertNotEquals(Thread.currentThread(), kernel);
                Continuation.yield(scope);
                System.out.println("resume yield, Continuation run in " + Thread.currentThread().getName());
                assertNotEquals(Thread.currentThread(), kernel);
            }
        };
        Continuation cont = new Continuation(scope, target);
        System.out.println("Continuation create in " + Thread.currentThread().getName());
        assertEquals(Thread.currentThread(), kernel);
        Thread thread = new Thread(){
            public void run() {
                cont.run();
                cont.run();
            }
        };
        thread.start();
        thread.join();
    }

    static void bar() throws Exception {
        final Thread kernel = Thread.currentThread();
        Runnable target = new Runnable() {
            public void run() {
                System.out.println("before yield, Continuation run in " + Thread.currentThread().getName());
                assertEquals(Thread.currentThread().getName(), "bar-thread-0");
                Continuation.yield(scope);
                System.out.println("resume yield, Continuation run in " + Thread.currentThread().getName());
                assertEquals(Thread.currentThread(), kernel);
            }
        };
        Continuation cont = new Continuation(scope, target);
        System.out.println("Continuation create in " + Thread.currentThread().getName());
        assertEquals(Thread.currentThread(), kernel);
        Thread thread = new Thread("bar-thread-0"){
            public void run() {
                cont.run();
            }
        };
        thread.start();
        thread.join();
        cont.run();
    }

    // create in other thread and run in main
    static Continuation internal;
    static void qux() throws Exception {
        final Thread kernel = Thread.currentThread();
        Runnable target = new Runnable() {
            public void run() {
                System.out.println("before yield, Continuation run in " + Thread.currentThread().getName());
                assertEquals(Thread.currentThread(), kernel);
                Continuation.yield(scope);
                System.out.println("resume yield, Continuation run in " + Thread.currentThread().getName());
                assertEquals(Thread.currentThread(), kernel);
            }
        };
        Thread thread = new Thread("qux-thread-0"){
            public void run() {
                Continuation cont = new Continuation(scope, target);
                System.out.println("Continuation create in " + Thread.currentThread().getName());
                assertEquals(Thread.currentThread().getName(), "qux-thread-0");
                internal = cont;
            }
        };
        thread.start();
        thread.join();
        Thread.sleep(100);
        internal.run();
        internal.run();
    }

    static void baz() throws Exception {
        final Thread kernel = Thread.currentThread();
        Runnable target = new Runnable() {
            public void run() {
                System.out.println("before yield, Continuation run in " + Thread.currentThread().getName());
                assertEquals(Thread.currentThread(), kernel);
                Continuation.yield(scope);
                System.out.println("resume yield, Continuation run in " + Thread.currentThread().getName());
                assertEquals(Thread.currentThread().getName(), "baz-thread-0");
            }
        };
        Thread thread = new Thread("baz-thread-0"){
            public void run() {
                Continuation cont = new Continuation(scope, target);
                System.out.println("Continuation create in " + Thread.currentThread().getName());
                assertEquals(Thread.currentThread().getName(), "baz-thread-0");
                internal = cont;
                try {
                    Thread.sleep(2000);
                } catch (Exception e) {
                }
                cont.run();
            }
        };
        thread.start();
        Thread.sleep(1000);
        internal.run();
        thread.join();
    }


    static void switch3threads() throws Exception {
        Runnable target = new Runnable() {
            public void run() {
                System.out.println("before yield, Continuation run in " + Thread.currentThread().getName());
                assertEquals(Thread.currentThread().getName(), "second-thread");
                Continuation.yield(scope);
                System.out.println("resume yield, Continuation run in " + Thread.currentThread().getName());
                assertEquals(Thread.currentThread().getName(), "third-thread");
            }
        };
        Thread thread = new Thread("first-thread"){
            public void run() {
                Continuation cont = new Continuation(scope, target);
                System.out.println("Continuation create in " + Thread.currentThread().getName());
                assertEquals(Thread.currentThread().getName(), "first-thread");
                internal = cont;
            }
        };
        thread.start();
        thread.join();
        Thread thread1 = new Thread("second-thread"){
            public void run() {
                internal.run();
            }
        };
        thread1.start();
        thread1.join();
        Thread thread2 = new Thread("third-thread"){
            public void run() {
                internal.run();
            }
        };
        thread2.start();
        thread2.join();
    }
}
