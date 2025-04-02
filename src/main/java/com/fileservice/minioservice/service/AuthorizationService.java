package com.fileservice.minioservice.service;

import com.fileservice.minioservice.exception.FileServiceException;
import com.fileservice.minioservice.model.FgaRelation;
import dev.openfga.sdk.api.client.OpenFgaClient;
import dev.openfga.sdk.api.client.model.ClientCheckRequest;
import dev.openfga.sdk.api.client.model.ClientReadRequest;
import dev.openfga.sdk.api.client.model.ClientWriteRequest;
import dev.openfga.sdk.api.client.model.TupleKey;
import dev.openfga.sdk.api.client.model.UserType;
import dev.openfga.sdk.api.model.CheckResponse;
import dev.openfga.sdk.api.model.ReadResponse;
import dev.openfga.sdk.api.model.Tuple;
import dev.openfga.sdk.api.model.TupleChange;
import dev.openfga.sdk.api.model.TupleOperation;
import dev.openfga.sdk.api.model.WriteResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorizationService {

    private final OpenFgaClient openFgaClient;
    
    // Object types
    private static final String FILE_TYPE = "file";
    private static final String DIRECTORY_TYPE = "directory";
    
    // Relation types
    private static final String RELATION_OWNER = "owner";
    private static final String RELATION_READER = "reader";
    private static final String RELATION_WRITER = "writer";
    private static final String RELATION_DELETER = "deleter";
    
    /**
     * Check if a user has a specific permission on an object
     */
    public boolean checkPermission(String objectType, String objectId, String relation, String username) {
        try {
            String object = objectType + ":" + objectId;
            String user = "user:" + username;
            
            ClientCheckRequest request = new ClientCheckRequest()
                    .user(user)
                    .relation(relation)
                    .object(object);
            
            CheckResponse response = openFgaClient.check(request).get();
            return response.getAllowed();
        } catch (Exception e) {
            log.error("Error checking permission: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Add a relation between a user and an object
     */
    public void addRelation(String objectType, String objectId, String relation, String username) {
        try {
            String object = objectType + ":" + objectId;
            String user = "user:" + username;
            
            TupleKey tupleKey = new TupleKey()
                    .object(object)
                    .relation(relation)
                    .user(user);
            
            TupleChange tupleChange = new TupleChange()
                    .tupleKey(tupleKey)
                    .operation(TupleOperation.WRITE);
            
            ClientWriteRequest request = new ClientWriteRequest()
                    .writes(Collections.singletonList(tupleChange));
            
            WriteResponse response = openFgaClient.write(request).get();
            log.info("Added relation: {} {} {}", object, relation, user);
        } catch (Exception e) {
            log.error("Error adding relation: {}", e.getMessage(), e);
            throw new FileServiceException("Failed to add permission", "PERMISSION_ERROR", e);
        }
    }
    
    /**
     * Remove a relation between a user and an object
     */
    public void removeRelation(String objectType, String objectId, String relation, String username) {
        try {
            String object = objectType + ":" + objectId;
            String user = "user:" + username;
            
            TupleKey tupleKey = new TupleKey()
                    .object(object)
                    .relation(relation)
                    .user(user);
            
            TupleChange tupleChange = new TupleChange()
                    .tupleKey(tupleKey)
                    .operation(TupleOperation.DELETE);
            
            ClientWriteRequest request = new ClientWriteRequest()
                    .deletes(Collections.singletonList(tupleChange));
            
            WriteResponse response = openFgaClient.write(request).get();
            log.info("Removed relation: {} {} {}", object, relation, user);
        } catch (Exception e) {
            log.error("Error removing relation: {}", e.getMessage(), e);
            throw new FileServiceException("Failed to remove permission", "PERMISSION_ERROR", e);
        }
    }
    
    /**
     * Get all relations for an object
     */
    public List<FgaRelation> getRelationsForObject(String objectType, String objectId) {
        try {
            String object = objectType + ":" + objectId;
            
            ClientReadRequest request = new ClientReadRequest()
                    .object(object);
            
            ReadResponse response = openFgaClient.read(request).get();
            List<FgaRelation> relations = new ArrayList<>();
            
            for (Tuple tuple : response.getTuples()) {
                FgaRelation relation = FgaRelation.builder()
                        .object(tuple.getKey().getObject())
                        .relation(tuple.getKey().getRelation())
                        .user(tuple.getKey().getUser())
                        .build();
                relations.add(relation);
            }
            
            return relations;
        } catch (Exception e) {
            log.error("Error getting relations for object: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get all relations for a user
     */
    public List<FgaRelation> getRelationsForUser(String username) {
        try {
            String user = "user:" + username;
            
            ClientReadRequest request = new ClientReadRequest()
                    .user(user);
            
            ReadResponse response = openFgaClient.read(request).get();
            List<FgaRelation> relations = new ArrayList<>();
            
            for (Tuple tuple : response.getTuples()) {
                FgaRelation relation = FgaRelation.builder()
                        .object(tuple.getKey().getObject())
                        .relation(tuple.getKey().getRelation())
                        .user(tuple.getKey().getUser())
                        .build();
                relations.add(relation);
            }
            
            return relations;
        } catch (Exception e) {
            log.error("Error getting relations for user: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Initialize owner permissions for a file
     */
    public void initializeFileOwner(String fileId, String username) {
        addRelation(FILE_TYPE, fileId, RELATION_OWNER, username);
        addRelation(FILE_TYPE, fileId, RELATION_READER, username);
        addRelation(FILE_TYPE, fileId, RELATION_WRITER, username);
        addRelation(FILE_TYPE, fileId, RELATION_DELETER, username);
    }
    
    /**
     * Check if user has read permission on a file
     */
    public boolean canReadFile(String fileId, String username) {
        return checkPermission(FILE_TYPE, fileId, RELATION_READER, username);
    }
    
    /**
     * Check if user has write permission on a file
     */
    public boolean canWriteFile(String fileId, String username) {
        return checkPermission(FILE_TYPE, fileId, RELATION_WRITER, username);
    }
    
    /**
     * Check if user has delete permission on a file
     */
    public boolean canDeleteFile(String fileId, String username) {
        return checkPermission(FILE_TYPE, fileId, RELATION_DELETER, username);
    }
    
    /**
     * Grant read permission on a file to a user
     */
    public void grantReadPermission(String fileId, String username) {
        addRelation(FILE_TYPE, fileId, RELATION_READER, username);
    }
    
    /**
     * Grant write permission on a file to a user
     */
    public void grantWritePermission(String fileId, String username) {
        addRelation(FILE_TYPE, fileId, RELATION_WRITER, username);
    }
    
    /**
     * Grant delete permission on a file to a user
     */
    public void grantDeletePermission(String fileId, String username) {
        addRelation(FILE_TYPE, fileId, RELATION_DELETER, username);
    }
    
    /**
     * Revoke read permission on a file from a user
     */
    public void revokeReadPermission(String fileId, String username) {
        removeRelation(FILE_TYPE, fileId, RELATION_READER, username);
    }
    
    /**
     * Revoke write permission on a file from a user
     */
    public void revokeWritePermission(String fileId, String username) {
        removeRelation(FILE_TYPE, fileId, RELATION_WRITER, username);
    }
    
    /**
     * Revoke delete permission on a file from a user
     */
    public void revokeDeletePermission(String fileId, String username) {
        removeRelation(FILE_TYPE, fileId, RELATION_DELETER, username);
    }
}
