package com.dev.pojo;

import com.dev.pojo.Comment;
import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="EclipseLink-2.7.9.v20210604-rNA", date="2023-09-11T07:35:45")
@StaticMetamodel(CommentLevel.class)
public class CommentLevel_ { 

    public static volatile SingularAttribute<CommentLevel, Integer> level;
    public static volatile SingularAttribute<CommentLevel, Comment> commentId;
    public static volatile SingularAttribute<CommentLevel, Integer> id;
    public static volatile SingularAttribute<CommentLevel, Comment> parentId;

}