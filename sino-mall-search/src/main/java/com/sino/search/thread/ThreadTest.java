package com.sino.search.thread;

import java.util.concurrent.*;

public class ThreadTest {
    public static ExecutorService executor = Executors.newFixedThreadPool(10);

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        // runAsync  没有返回结果
//        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getName());
//            int i = 10 / 2;
//            System.out.println("运行结果：" + i);
//        }, executor);

        // supplyAsync  可以获取返回结果
//        CompletableFuture<Integer> supplyAsync = CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getName());
//            int i = 10 / 0;
//            System.out.println("运行结果：" + i);
//            return i;
//        }, executor).whenComplete((res,exception) -> {
//            // 虽然能得到异常信息，但是无法修改返回数据
//            System.out.println("异步任务成功完成了。。。。结果是" + res + "异常："+ exception);
//        }).exceptionally((e)->{
//            // 出现异常后可以感知且可以返回默认值
//            System.out.println("出现异常："+e);
//            return 0;
//        });

        /**
         * 方法执行完后的处理 感知异常和返回结果
         */
//        CompletableFuture<Integer> supplyAsync = CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getName());
//            int i = 10 / 10;
//            System.out.println("运行结果：" + i);
//            return i;
//        }, executor).handle((res, exception) -> {
//            if (res != null) {
//                return res;
//            }
//            if (exception != null) {
//                return 0;
//            }
//            return res;
//        });


        /**
         * 线程串行化
         *         .thenRun()    //无上一步返回结果，也无返回值    async不共用线程 不i加async是共用同一个县线程
         *               .thenRunAsync(()->{
         *                    System.out.println("任务2启动了。。。");
         *               },executor);
         *         .thenAccept() //有上一步返回结果，但无本次返回值    async不共用线程 不i加async是共用同一个县线程
         *               .thenAcceptAsync((res)->{
         *                   System.out.println("任务2启动了。。。返回结果是"+res);
         *               },executor);
         *         .thenApply()  //有上一步返回结果，也有本次返回值
         *                   .thenApplyAsync((res) -> {
         *                       System.out.println("任务2启动了。。。返回结果是" + res);
         *                       return res * 2;
         *                   }, executor);
         */
//        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
//            System.out.println("当前线程：" + Thread.currentThread().getName());
//            int i = 10 / 10;
//            System.out.println("运行结果：" + i);
//            return i;
//        }, executor).thenApplyAsync((res) -> {
//            System.out.println("任务2启动了。。。返回结果是" + res);
//            return res * 2;
//        }, executor);


        /**
         * 两个任务都完成
         */
//        CompletableFuture<Object> future01 = CompletableFuture.supplyAsync(() -> {
//            System.out.println("任务1开始：" + Thread.currentThread().getName());
//            int i = 10 / 10;
//            System.out.println("任务1结束：" + i);
//            return i;
//        }, executor);

//        CompletableFuture<Object> future02 = CompletableFuture.supplyAsync(() -> {
////            try {
////                Thread.sleep(3000);
////            } catch (InterruptedException e) {
////                e.printStackTrace();
////            }
//            System.out.println("任务2开始：" + Thread.currentThread().getName());
//            System.out.println("任务2结束：");
//            return "hello";
//        }, executor);

        // runAfterBothAsync ==》无法感知任务结果
//        future01.runAfterBothAsync(future02,()->{
//            System.out.println("任务3开始执行");
//        },executor);

        // thenAcceptBothAsync ==》 可以接收两个线程任务执行的结果
//        future01.thenAcceptBothAsync(future02,(res1,res2)->{
//            System.out.println("任务3执行。。。res1="+res1+"...res2="+res2);
//        },executor);

        // thenCombineAsync ==》得到两个线程任务结果处理后返回最终结果
//        CompletableFuture<String> future = future01.thenCombineAsync(future02, (res1, res2) -> {
//            return res1 +"/"+ res2;
//        }, executor);

        /**
         * 两个任务，只要有一个完成，我们就执行任务3
         * runAfterEitherAsync --> 不感知结果，自己也没返回值
         */
//        future01.runAfterEitherAsync(future02,()->{
//            System.out.println("任务3执行。。。=");
//        },executor);

        /**
         * 只接收最先执行完成的任务的结果
         * acceptEitherAsync --> 可以感知结果，自己无返回值
         */
//        future01.acceptEitherAsync(future02,(res)->{
//            System.out.println("任务3执行。。。="+res);
//        },executor);


        /**
         *  applyToEitherAsync --> 可以获取结果，也可以返回结果
         */
//        CompletableFuture<String> future = future01.applyToEitherAsync(future02, (res) -> {
//            System.out.println("任务3执行。。。=" + res);
//            return "任务3执行结束。。。=" + res;
//        }, executor);

        //多任务组合
        CompletableFuture<String> futureImg = CompletableFuture.supplyAsync(() -> {
            System.out.println("查询商品图片信息" + Thread.currentThread().getName());
            return "hello.jpg";
        }, executor);

        CompletableFuture<String> futureAttr = CompletableFuture.supplyAsync(() -> {
            System.out.println("查询商品图属性" + Thread.currentThread().getName());
            return "黑色+232G";
        }, executor);

        CompletableFuture<String> futureDesc = CompletableFuture.supplyAsync(() -> {
            System.out.println("查询商品介绍" + Thread.currentThread().getName());
            return "华为.jpg";
        }, executor);

//        CompletableFuture<Void> allOf = CompletableFuture.allOf(futureImg, futureAttr, futureDesc);
//        allOf.get();  // 需要等待三个任务同时完成

        // 有任意一个成功就返回
        CompletableFuture<Object> anyOf = CompletableFuture.anyOf(futureImg, futureAttr, futureDesc);


        System.out.println("supplyAsync结果：futureImg" +   anyOf.get());
//        System.out.println("supplyAsync结果：futureImg" + futureImg.get() +"futureAttr"+futureAttr.get());


    }


    public static void threadTest(String[] args) throws ExecutionException, InterruptedException {
        System.out.println("main....start...." + Thread.currentThread().getName());
        /**
         * 1. 继承Thread
         *         Thread01 thread01 = new Thread01();
         *         thread01.start();
         * 2. 实现Runnable 接口
         *         Runnable01 runnable01 = new Runnable01();
         *         Thread thread = new Thread(runnable01);
         *         thread.start();
         * 3. 实现Callable 接口 + FutureTask (可以拿到返回值结果，可以处理异常)
         *         FutureTask<Integer> task = new FutureTask<>(new Callable01());
         *         Thread thread = new Thread(task);
         *         thread.start();
         *         Integer o = task.get();
         *         System.out.println("运行结果。。。 "+ o);
         * 4. 线程池【ExecutorService】
         *         给线程池提交任务 executorService.execute(new Runnable01());
         *         创建方式；
         *              1. Executes
         *              2. new ThreadPoolExecutor(七大参数)
         *
         * Future可以获取到异步结果
         *
         * 四种创建线程的比较
         *      1，2不能得到返回值，3可以获得返回值
         *      4.可以控制资源，性能稳定
         */

        // 当前系统中池只有一两个，每个异步任务提交给线程池让他自己去执行就行
//        executorService.submit()    //提交一个任务，并且有返回值
//        executorService.execute(new Runnable01());    //执行一个任务，无返回值

        /**
         * 线程池的七大参数
         * int corePoolSize,  //核心线程数
         * int maximumPoolSize,    //最大线程数
         * long keepAliveTime,     //存活时间
         * TimeUnit unit,          //时间单位
         * BlockingQueue<Runnable> workQueue,  //工作队列
         * ThreadFactory threadFactory,        //线程工程
         * RejectedExecutionHandler handler    //拒绝策略
         *
         * 工作顺序/运行流程
         * 1. 线程池创建，准备好core数量的核心线程，准备接收任务
         * 1.1 如果core满了，就将再进来的任务放入阻塞队列中，空闲的core就会自己去阻塞队列获取任务执行
         * 2 阻塞队列满了，就直接开新线程执行，最大只能开到max指定的数量
         * 3 max满了就用RejectedExecutionHandler拒绝任务
         * 3.1 max都执行完成，有很多空闲，在指定时间keepAliveTime以后，释放max-core这些线程
         *
         * 面试题
         * 一个线程池 core7,max20,queue50,100并发进来怎么分配
         * 7个立即执行，然后50个进入队列，在13个执行，剩余30将会执行拒绝策略
         *
         */
        new ThreadPoolExecutor(5,
                200,
                10,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(100000),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());

//        Executors.newCachedThreadPool();  //core是0， 所有都可以回收
//        Executors.newFixedThreadPool();   //固定大小，core=max;都不可回收
//        Executors.newScheduledThreadPool(); //定时任务的线程池
//        Executors.newSingleThreadExecutor();//单线程的线程池，后台从队列里面获取任务，挨个执行

        System.out.println("main....end...." + Thread.currentThread().getName());

    }


    public static class Thread01 extends Thread {

        @Override
        public void run() {
            System.out.println("当前线程：" + Thread.currentThread().getName());
            int i = 10 / 2;
            System.out.println("运行结果：" + i);

        }
    }

    public static class Runnable01 implements Runnable {

        @Override
        public void run() {
            System.out.println("当前线程：" + Thread.currentThread());
            int i = 10 / 2;
            System.out.println("运行结果：" + i);

        }
    }

    public static class Callable01 implements Callable<Integer> {


        @Override
        public Integer call() throws Exception {
            System.out.println("当前线程：" + Thread.currentThread());
            int i = 10 / 2;
            System.out.println("运行结果：" + i);
            return i;
        }
    }
}
