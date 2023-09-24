package com.sino.search.service;

import com.sino.search.vo.SearchParam;
import com.sino.search.vo.SearchResult;

public interface MallSearchService {
    /**
     *
     * @param param 检索的所有参数
     * @return {@link Object} 返回检索的结果
     */

    SearchResult search(SearchParam param);
}
