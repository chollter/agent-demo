package cn.chollter.agent.demo.repository;

import cn.chollter.agent.demo.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 文档数据访问接口
 */
@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    /**
     * 根据标题搜索文档
     */
    List<Document> findByTitleContainingIgnoreCase(String title);

    /**
     * 根据内容搜索文档
     */
    List<Document> findByContentContainingIgnoreCase(String content);

    /**
     * 根据标题和内容搜索文档
     */
    List<Document> findByTitleContainingOrContentContainingAllIgnoreCase(String title, String content);
}
