package com.sino.search.controller;

import com.sino.search.service.MallSearchService;
import com.sino.search.vo.SearchParam;
import com.sino.search.vo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

@Controller
public class SearchController {

    @Autowired
    private MallSearchService mallSearchService;

    /**
     * 将页面提交古来的所有请求参数封装成指定的对象
     * @param param
     * @return {@link String}
     */

    @GetMapping("/list.html")
    public String listPage(SearchParam param, Model model, HttpServletRequest request){
        String queryString = request.getQueryString();
        param.set_queryString(queryString);
        SearchResult result = mallSearchService.search(param);
        model.addAttribute("result",result);
        return "list";
    }
}
