package com.sino.product.web;

import com.sino.product.entity.CategoryEntity;
import com.sino.product.service.CategoryService;
import com.sino.product.vo.Catelog2Vo;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Controller
public class IndexController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private RedisTemplate redisTemplate;

    @GetMapping({"/", "/index.html"})
    public String indexPage(Model model){

        //TODO 1. 查出所有的一级分类
        List<CategoryEntity> categoryEntities = categoryService.getLevel1Categorys();
        model.addAttribute("categorys", categoryEntities);
        return "index";
    }

    //index/json/catalog.json
    @ResponseBody
    @GetMapping("/index/json/catalog.json")
    public Map<String, List<Catelog2Vo>> getCatalogJson(){
        return categoryService.getCatalogJson();
    }




    //--------------------------Redisson分布式锁测试---------------------------------------------------------

    /**
     * 【分布式锁】
     * @return
     */
    @ResponseBody
    @GetMapping("/hello")
    public String hello(){
        RLock lock = redissonClient.getLock("my-lock");

        // 2.枷锁
//        lock.lock();
        // 1） 锁的自动续期，如果业务超长，运行期间自动给锁续上新的30s，不用担心业务时间长，锁自动过期被删掉
        // 2） 枷锁的业务只要运行完成，就不会给当前锁续期，即使不手动解锁，锁默认在30s以后自动删除

        lock.lock(10, TimeUnit.SECONDS); // 10s秒自动解锁，自动解锁时间一定要大于业务执行的时间
        // 问题 ， lock.lock(10, TimeUnit.SECONDS); 在所时间到了以后，不会自动续期
        // 1. 如果我们传递了锁的超时时间，就发送给redis执行脚本，惊醒占锁，默认超时就是我们指定的时间
        // 2. 如果我们未指定锁的超时时间，就是用30 * 1000【LockWatchdogTimeout看门狗默认的时间】
        //    只要占锁成功，就会启动一个定时任务【重新给锁设置过期时间，新的过期时间就是看门狗的默认时间】，每隔10s都会自动再次续期，续成30s、
        //    internalLockLeaseTinme[看门狗时间]/3 ，10s

        // 最佳实战
        // 1. lock.lock(30, TimeUnit.SECONDS); 省掉了整个续期操作, 手动解锁
        try {
            System.out.println("加锁成功，执行业务。。。"+ Thread.currentThread().getId());
            Thread.sleep(30000L);
        }catch (Exception e){

        }finally {
            System.out.println("释放锁。。。"+Thread.currentThread().getId());
            lock.unlock();
        }

        return "hello";
    }

    /**
     * 【分布式锁 -读写锁】
     * 保证一定能督导最新的数据，修改期间，写锁是一个排他锁（互斥锁，。独享锁）。读锁是一个共享锁
     * 写锁没释放读就必修等待
     * 读+读，相当于无锁，并发读，只会在redis中记录好，所有当前的读锁，他们都会同时加锁成功
     * 写+读，等待写锁释放
     * 写+写，阻塞方式
     * 读+写，有读锁，写也需要等待
     * 只要有写锁的存在，都必须等待
     * @return {@link String}
     */

    @GetMapping("/write")
    @ResponseBody
    public String writeValue(){

        // 获取一个读写锁
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("rw-lock");
        // 获取写锁
        RLock wLock = readWriteLock.writeLock();
        // 修改数据加写锁
        wLock.lock();
        try {
            System.out.println("写锁枷锁成功。。。"+ Thread.currentThread().getId());
            String s = UUID.randomUUID().toString();
            Thread.sleep(30000);
            redisTemplate.opsForValue().set("writeValue", s);
            return s;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            wLock.unlock();
            System.out.println("写锁释放。。。"+ Thread.currentThread().getId());
        }
        return null;
    }

    @GetMapping("/read")
    @ResponseBody
    public String readValue(){

        // 获取一个读写锁
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock("rw-lock");
        // 获取写锁
        RLock rLock = readWriteLock.readLock();
        // 修改数据加写锁
        rLock.lock();
        try {
            System.out.println("读锁枷锁成功。。。"+ Thread.currentThread().getId());
            return (String) redisTemplate.opsForValue().get("writeValue");
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            rLock.unlock();
            System.out.println("读锁释放。。。"+ Thread.currentThread().getId());
        }
        return null;
    }


    /**
     * 车库停车【分布式锁 -信号量】
     * 3车位
     * 信号量也可以用作分布式限流
     *
     * @return {@link String}
     * @throws InterruptedException
     */

    @ResponseBody
    @GetMapping("/park")
    public String park() throws InterruptedException {
        RSemaphore park = redissonClient.getSemaphore("park");
//        park.acquire();  // 获取一个信号，获取一个值，占一个车位

        boolean b = park.tryAcquire();
        if (b) {
            // 执行业务
        }else {
            return "error";
        }

        return "ok ==>" + b;
    }

    @ResponseBody
    @GetMapping("/go")
    public String go() throws InterruptedException {
        RSemaphore park = redissonClient.getSemaphore("park");
        park.release(); //释放一个车位

        return "ok";
    }


    /**
     * 放假锁门 【分布式锁 -闭锁】
     * 1班人没了，2班人没了。。。
     * 5个班都走了，我们可以锁大门
     * @return {@link String}
     * @throws InterruptedException
     */

    @ResponseBody
    @GetMapping("/lockDoor")
    public String lockDoor() throws InterruptedException {
        RCountDownLatch door = redissonClient.getCountDownLatch("door");
        door.trySetCount(5L);
        door.await(); //等待闭锁都完成

        return "放假了";
    }

    @ResponseBody
    @GetMapping("/gogogo/{id}")
    public String gogogo(@PathVariable("id") Long id) throws InterruptedException {
        RCountDownLatch door = redissonClient.getCountDownLatch("door");
        door.countDown(); // 记数减一

        return id + "班的人都走了。。。";
    }


}
