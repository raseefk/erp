package com.supererp.erp.repository;

import com.supererp.erp.entity.DocumentFolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentFolderRepository extends JpaRepository<DocumentFolder, Long> {

    @Query("SELECT f FROM DocumentFolder f WHERE f.parentFolder IS NULL AND f.active = true ORDER BY f.name")
    List<DocumentFolder> findRootFolders();

    @Query("SELECT f FROM DocumentFolder f WHERE f.parentFolder.id = :parentId AND f.active = true ORDER BY f.name")
    List<DocumentFolder> findByParentFolderId(Long parentId);

    @Query("SELECT f FROM DocumentFolder f WHERE f.active = true ORDER BY f.name")
    List<DocumentFolder> findAllActive();
}
