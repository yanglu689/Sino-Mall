package com.sino.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.sino.product.dao.CategoryBrandRelationDao;
import com.sino.product.vo.Catelog2Vo;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sino.common.utils.PageUtils;
import com.sino.common.utils.Query;

import com.sino.product.dao.CategoryDao;
import com.sino.product.entity.CategoryEntity;
import com.sino.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    private CategoryBrandRelationDao categoryBrandRelationDao;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 树型列表
     *
     * @return {@link List}<{@link CategoryEntity}>
     */
    @Override
    public List<CategoryEntity> listWithTree() {
        // 查询所有的菜单
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);

        // 将菜单按照树形结构存放
        List<CategoryEntity> categoryEntityTree = categoryEntities.stream()
                .filter(item -> item.getParentCid() == 0)
                .map(menu -> {
                    //设置子菜单
                    menu.setChildren(setChildern(menu, categoryEntities));
                    return menu;
                })
                // 按照sort字段排序
                .sorted(Comparator.comparingInt(categoryEntity -> categoryEntity.getSort() == null ? 0 : categoryEntity.getSort()))
                // 输出list
                .collect(Collectors.toList());
        return categoryEntityTree;
    }

    /**
     * 使用逻辑删除菜单通过类别id
     *
     * @param catIds 正如列表
     */

    @Override
    public void removeMenuByCatIds(List<Long> catIds) {
        //TODO  后期根据业务进行删除前的判断

        baseMapper.deleteBatchIds(catIds);
    }

    @Override
    public Long[] getCateLogIds(Long catelogId) {
        List<Long> cateLogIds = new ArrayList();
        cateLogIds = findParentPath(catelogId, cateLogIds);

        Collections.reverse(cateLogIds);

        return cateLogIds.toArray(new Long[cateLogIds.size()]);
    }


    /**
     * updateCascade
     *
     *  更新数据后删除缓存的两种方式
     *  1。 @Caching(evict = {
     *             @CacheEvict(value = {"category"}, key = "'getLevel1Categorys'"),
     *             @CacheEvict(value = {"category"}, key = "'getCatalogJson'")
     *     })
     *  2. @CacheEvict(value = {"category"}, allEntries = true)
     *
     * @param category
     */

//    @CacheEvict(value = {"category"}, key = "'getLevel1Categorys'")
//    @Caching(evict = {
//            @CacheEvict(value = {"category"}, key = "'getLevel1Categorys'"),
//            @CacheEvict(value = {"category"}, key = "'getCatalogJson'")
//    })
    @CacheEvict(value = {"category"}, allEntries = true)
//    @CachePut //双写
    @Transactional
    @Override
    public void updateDetail(CategoryEntity category) {
        // 跟新基本信息
        this.updateById(category);
        //判断是否修改关联表的分类名称
        if (StringUtils.isNotEmpty(category.getName())) {
            categoryBrandRelationDao.updateCateLogName(category.getCatId(), category.getName());
            //TODO 其他关联属性修改
        }


    }


    /**
     * 1. 每一个需要缓存的数据我们都来指定放到哪个名字的缓存。【缓存的分区{按照业务类型划分}】
     * 2.代表当前的返回结果需要缓存，如果有缓存，，方法不调用，如果缓存中没有会调用方法，最后将方法的返回结果放入缓存
     * 3. 默认行为
     * 1.如果缓存中有，方法不调用
     * 2. key默认自动生成，缓存名字::SimpleKey []  （自动生成的key值）
     * 3. 缓存的value的值，默认使用jdk序列化机制，将序列化后的数据存到redis
     * 4. 默认ttl（过期时间） -1 永不过期
     * 自定义属性：
     * 1. 指定生成的缓存使用的key , key属性指定，接收一个spEL
     * 2. 指定缓存中的数据存活时间， 配置文件中修改ttl  key = "'level1Categorys'"
     *
     * @return {@link List}<{@link CategoryEntity}>
     */

    @Cacheable(value = {"category"}, key = "#root.method.name")
    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        List<CategoryEntity> categoryEntities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        return categoryEntities;
    }

    private final String CATEGORY_LOCK = "catLock";

    @Cacheable(value = {"category"}, key = "#root.methodName", sync = true) // 开启同步
    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        // 查询数据库
        System.out.println("查询数据库。。。");

        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);

        List<CategoryEntity> level1Categorys = getChildren(categoryEntities, 0L);
        if (ObjectUtils.isEmpty(level1Categorys)) {
            return null;
        }

        Map<String, List<Catelog2Vo>> collect = level1Categorys.stream().collect(Collectors.toMap(l1 -> l1.getCatId().toString(), l1 -> {
            // 查找2级节点
            List<CategoryEntity> level2Categorys = getChildren(categoryEntities, l1.getCatId());
            if (ObjectUtils.isEmpty(level2Categorys)) {
                return new ArrayList<>();
            }

            return level2Categorys.stream().map(l2 -> {
                List<CategoryEntity> level3Categorys = getChildren(categoryEntities, l2.getCatId());
                List<Catelog2Vo.Catelog3Vo> catelog3Vos = null;
                if (!ObjectUtils.isEmpty(level3Categorys)) {
                    catelog3Vos = level3Categorys.stream().map(l3 -> {
                        // 封装三级分类
                        return new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                    }).collect(Collectors.toList());
                }

                return new Catelog2Vo(l1.getCatId().toString(), catelog3Vos, l2.getCatId().toString(), l2.getName());
            }).collect(Collectors.toList());
        }));

        return collect;
    }


    /**
     * TODO lettuce在5.0以下产生对外内存溢出：OutOfDirectMemoryError
     *
     * @return {@link Map}<{@link String}, {@link List}<{@link Catelog2Vo}>>
     */
//    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson2() {
        // 加入缓存逻辑
        String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
        if (StringUtils.isNotEmpty(catalogJSON)) {
            // 解析缓存中的三级分类[反序列化]
            Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {
            });
            System.out.println("缓存命中。。。");
            return result;
        }

        // 加双重加厕所 dcl
//        RLock catLock = redisson.getLock(CATEGORY_LOCK);
//        catLock.lock(3L, TimeUnit.SECONDS);
        return getCatalogJsonFromDbWithRedissonLock();

    }

    /**
     * 使用redisson方式实现分布式锁
     * <p>
     * 缓存里面的数据如何保证和数据库保持一致
     * 缓存数据库一致性问题
     *
     * @return {@link Map}<{@link String}, {@link List}<{@link Catelog2Vo}>>
     */

    private Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithRedissonLock() {

        // 1. 锁的名字。锁的粒度，越细越快
        // 锁的粒度：具体缓存的是某个数据， 11-号商品：product-11-lock product-12-lock > product-lock
        RLock lock = redissonClient.getLock("catalog-lock");
        lock.lock();

        try {
            String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
            if (StringUtils.isNotEmpty(catalogJSON)) {
                // 解析缓存中的三级分类[反序列化]
                Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {
                });
                return result;
            }

            // 缓存中没有数据，查询数据库
            Map<String, List<Catelog2Vo>> catalogJsonFromDb = getCatalogJsonFromDb();
            if (ObjectUtils.isEmpty(catalogJsonFromDb)) {
                redisTemplate.opsForValue().set("catalogJSON", null, 24 + RandomUtils.nextInt(new Random()), TimeUnit.HOURS);
            } else {
                String jsonString = JSON.toJSONString(catalogJsonFromDb);
                // 查到的数据在放入缓存 【序列化】
                redisTemplate.opsForValue().set("catalogJSON", jsonString, 24, TimeUnit.HOURS);
            }
            return catalogJsonFromDb;

        } finally {
            lock.unlock();
        }
    }

    /**
     * 使用redis的lua脚本方式实现分布式锁
     *
     * @return
     */
    private Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithRedisLock() {
        String uuid = UUID.randomUUID().toString();
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 300, TimeUnit.SECONDS);

        if (null != lock && lock) {
            System.out.println("获取分布式锁成功。。。");
            try {
                String catalogJSON = redisTemplate.opsForValue().get("catalogJSON");
                if (StringUtils.isNotEmpty(catalogJSON)) {
                    // 解析缓存中的三级分类[反序列化]
                    Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJSON, new TypeReference<Map<String, List<Catelog2Vo>>>() {
                    });
                    return result;
                }

                // 缓存中没有数据，查询数据库
                Map<String, List<Catelog2Vo>> catalogJsonFromDb = getCatalogJsonFromDb();
                if (ObjectUtils.isEmpty(catalogJsonFromDb)) {
                    redisTemplate.opsForValue().set("catalogJSON", null, 24 + RandomUtils.nextInt(new Random()), TimeUnit.HOURS);
                } else {
                    String jsonString = JSON.toJSONString(catalogJsonFromDb);
                    // 查到的数据在放入缓存 【序列化】
                    redisTemplate.opsForValue().set("catalogJSON", jsonString, 24, TimeUnit.HOURS);
                }
                return catalogJsonFromDb;

            } finally {
//            catLock.unlock();
                String script = "if redis.call(\"get\",KEYS[1]) == ARGV[1]\n" +
                        "then\n" +
                        "    return redis.call(\"del\",KEYS[1])\n" +
                        "else\n" +
                        "    return 0\n" +
                        "end";

                // 删除锁
                Long lock1 = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList("lock"), uuid);
            }
        } else {
            System.out.println("获取分布式锁失败。。。等待重试");
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return getCatalogJsonFromDbWithRedisLock();
        }
    }

    /**
     * 从数据库中查询三级缓存
     *
     * @return {@link Map}<{@link String}, {@link List}<{@link Catelog2Vo}>>
     */

    @Transactional
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDb() {
        // 查询数据库
        System.out.println("查询数据库。。。");

        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);

        List<CategoryEntity> level1Categorys = getChildren(categoryEntities, 0L);
        if (ObjectUtils.isEmpty(level1Categorys)) {
            return null;
        }

        Map<String, List<Catelog2Vo>> collect = level1Categorys.stream().collect(Collectors.toMap(l1 -> l1.getCatId().toString(), l1 -> {
            // 查找2级节点
            List<CategoryEntity> level2Categorys = getChildren(categoryEntities, l1.getCatId());
            if (ObjectUtils.isEmpty(level2Categorys)) {
                return new ArrayList<>();
            }

            return level2Categorys.stream().map(l2 -> {
                List<CategoryEntity> level3Categorys = getChildren(categoryEntities, l2.getCatId());
                List<Catelog2Vo.Catelog3Vo> catelog3Vos = null;
                if (!ObjectUtils.isEmpty(level3Categorys)) {
                    catelog3Vos = level3Categorys.stream().map(l3 -> {
                        // 封装三级分类
                        return new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                    }).collect(Collectors.toList());
                }

                return new Catelog2Vo(l1.getCatId().toString(), catelog3Vos, l2.getCatId().toString(), l2.getName());
            }).collect(Collectors.toList());
        }));

        return collect;
    }

    private List<CategoryEntity> getChildren(List<CategoryEntity> categoryEntities, Long parentId) {
        List<CategoryEntity> children = categoryEntities.stream().filter(item -> parentId.equals(item.getParentCid())).collect(Collectors.toList());
//        return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", l1.getCatId()));
        return children;
    }


    /**
     * 找到父路径
     *
     * @param catelogId  catelog id
     * @param cateLogIds 美食日志id
     * @return {@link List}<{@link Long}>
     */

    public List<Long> findParentPath(long catelogId, List<Long> cateLogIds) {
        cateLogIds.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if (!ObjectUtils.isEmpty(byId) && byId.getParentCid() != 0) {
            this.findParentPath(byId.getParentCid(), cateLogIds);
        }
        return cateLogIds;
    }

    /**
     * 设置子菜单
     *
     * @param currMenu 当前菜单
     * @param allMenus 所有菜单
     * @return {@link List}<{@link CategoryEntity}>
     */
    private List<CategoryEntity> setChildern(CategoryEntity currMenu, List<CategoryEntity> allMenus) {

        return allMenus.stream()
                .filter(item -> item.getParentCid().equals(currMenu.getCatId()))
                .map(menu -> {
                    menu.setChildren(setChildern(menu, allMenus));
                    return menu;
                })
                .sorted(Comparator.comparingInt(categoryEntity -> categoryEntity.getSort() == null ? 0 : categoryEntity.getSort()))
                .collect(Collectors.toList());
    }

}