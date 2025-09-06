import React, { useState, useEffect } from 'react';
import { Table, Button, Modal, Form, Input, Select, Upload, message } from 'antd';
import { UploadOutlined } from '@ant-design/icons';
import apiClient from '../../services/apiClient';

const { Option } = Select;

export default function ContentManagement() {
  const [lessons, setLessons] = useState([]);
  const [signs, setSigns] = useState([]);
  const [loading, setLoading] = useState(false);
  const [lessonModalVisible, setLessonModalVisible] = useState(false);
  const [signModalVisible, setSignModalVisible] = useState(false);
  const [editingLesson, setEditingLesson] = useState(null);
  const [editingSign, setEditingSign] = useState(null);

  const [lessonForm] = Form.useForm();
  const [signForm] = Form.useForm();

  useEffect(() => {
    fetchLessons();
    fetchSigns();
  }, []);

  const fetchLessons = async () => {
    setLoading(true);
    try {
      const res = await apiClient.get('/api/admin/content/lessons');
      setLessons(res.data);
    } catch (e) {
      message.error('Failed to fetch lessons');
    } finally {
      setLoading(false);
    }
  };

  const fetchSigns = async () => {
    setLoading(true);
    try {
      const res = await apiClient.get('/api/admin/content/signs');
      setSigns(res.data);
    } catch (e) {
      message.error('Failed to fetch signs');
    } finally {
      setLoading(false);
    }
  };

  const openLessonModal = (lesson) => {
    setEditingLesson(lesson);
    lessonForm.resetFields();
    if (lesson) {
      lessonForm.setFieldsValue(lesson);
    }
    setLessonModalVisible(true);
  };

  const openSignModal = (sign) => {
    setEditingSign(sign);
    signForm.resetFields();
    if (sign) {
      signForm.setFieldsValue(sign);
    }
    setSignModalVisible(true);
  };

  const handleLessonSubmit = async (values) => {
    try {
      if (editingLesson) {
        await apiClient.put(`/api/admin/content/lessons/${editingLesson.id}`, values);
        message.success('Lesson updated successfully');
      } else {
        await apiClient.post('/api/admin/content/lessons', values);
        message.success('Lesson created successfully');
      }
      fetchLessons();
      setLessonModalVisible(false);
    } catch (e) {
      message.error('Failed to save lesson');
    }
  };

  const handleSignSubmit = async (values) => {
    const formData = new FormData();
    formData.append('sign', JSON.stringify({
      name: values.name,
      description: values.description,
      category: values.category
    }));
    if (values.video && values.video[0]) formData.append('video', values.video[0].originFileObj);
    if (values.image && values.image[0]) formData.append('image', values.image[0].originFileObj);

    try {
      if (editingSign) {
        await apiClient.put(`/api/admin/content/signs/${editingSign.id}`, formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        });
        message.success('Sign updated successfully');
      } else {
        await apiClient.post('/api/admin/content/signs', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        });
        message.success('Sign created successfully');
      }
      fetchSigns();
      setSignModalVisible(false);
    } catch (e) {
      message.error('Failed to save sign');
    }
  };

  const deleteLesson = async (id) => {
    try {
      await apiClient.delete(`/api/admin/content/lessons/${id}`);
      message.success('Lesson deleted');
      fetchLessons();
    } catch (e) {
      message.error('Failed to delete lesson');
    }
  };

  const deleteSign = async (id) => {
    try {
      await apiClient.delete(`/api/admin/content/signs/${id}`);
      message.success('Sign deleted');
      fetchSigns();
    } catch (e) {
      message.error('Failed to delete sign');
    }
  };

  const lessonColumns = [
    { title: 'Title', dataIndex: 'title', key: 'title' },
    { title: 'Category', dataIndex: 'category', key: 'category' },
    { title: 'Difficulty', dataIndex: 'difficulty', key: 'difficulty' },
    {
      title: 'Actions',
      key: 'actions',
      render: (_, record) => (
        <>
          <Button onClick={() => openLessonModal(record)} type="link">Edit</Button>
          <Button onClick={() => deleteLesson(record.id)} type="link" danger>Delete</Button>
        </>
      ),
    },
  ];

  const signColumns = [
    { title: 'Name', dataIndex: 'name', key: 'name' },
    { title: 'Category', dataIndex: 'category', key: 'category' },
    {
      title: 'Video',
      dataIndex: 'videoUrl',
      key: 'videoUrl',
      render: (url) => (url ? <a href={url} target="_blank" rel="noreferrer">View</a> : 'N/A'),
    },
    {
      title: 'Image',
      dataIndex: 'imageUrl',
      key: 'imageUrl',
      render: (url) => (url ? <img src={url} alt="Sign" width={50} /> : 'N/A'),
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_, record) => (
        <>
          <Button onClick={() => openSignModal(record)} type="link">Edit</Button>
          <Button onClick={() => deleteSign(record.id)} type="link" danger>Delete</Button>
        </>
      ),
    },
  ];

  return (
    <div>
      <h1>Content Management</h1>
      <Button type="primary" onClick={() => openLessonModal(null)}>Add Lesson</Button>
      <Table columns={lessonColumns} dataSource={lessons} rowKey="id" loading={loading} style={{ marginTop: 16 }} />
      
      <Button type="primary" onClick={() => openSignModal(null)} style={{ marginTop: 24 }}>Add Sign</Button>
      <Table columns={signColumns} dataSource={signs} rowKey="id" loading={loading} style={{ marginTop: 16 }} />

      <Modal
        title={editingLesson ? "Edit Lesson" : "Add Lesson"}
        visible={lessonModalVisible}
        onCancel={() => setLessonModalVisible(false)}
        onOk={() => lessonForm.submit()}
      >
        <Form form={lessonForm} layout="vertical" onFinish={handleLessonSubmit} initialValues={{ category: "General", difficulty: "Beginner" }}>
          <Form.Item name="title" label="Title" rules={[{ required: true, message: 'Please input the lesson title' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="Description">
            <Input.TextArea />
          </Form.Item>
          <Form.Item name="category" label="Category" rules={[{ required: true, message: 'Please select the category' }]}>
            <Select>
              <Option value="General">General</Option>
              <Option value="Other">Other</Option>
            </Select>
          </Form.Item>
          <Form.Item name="difficulty" label="Difficulty" rules={[{ required: true, message: 'Please select difficulty' }]}>
            <Select>
              <Option value="Beginner">Beginner</Option>
              <Option value="Intermediate">Intermediate</Option>
              <Option value="Advanced">Advanced</Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title={editingSign ? "Edit Sign" : "Add Sign"}
        visible={signModalVisible}
        onCancel={() => setSignModalVisible(false)}
        onOk={() => signForm.submit()}
      >
        <Form form={signForm} layout="vertical" onFinish={handleSignSubmit} initialValues={{ category: "General" }}>
          <Form.Item name="name" label="Name" rules={[{ required: true, message: 'Please input the sign name' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="description" label="Description">
            <Input.TextArea />
          </Form.Item>
          <Form.Item name="category" label="Category" rules={[{ required: true, message: 'Please select category' }]}>
            <Select>
              <Option value="General">General</Option>
              <Option value="Other">Other</Option>
            </Select>
          </Form.Item>
          <Form.Item name="video" label="Video" valuePropName="fileList" getValueFromEvent={e => e && e.fileList}>
            <Upload beforeUpload={() => false} maxCount={1} accept="video/*">
              <Button icon={<UploadOutlined />}>Select Video</Button>
            </Upload>
          </Form.Item>
          <Form.Item name="image" label="Image" valuePropName="fileList" getValueFromEvent={e => e && e.fileList}>
            <Upload beforeUpload={() => false} maxCount={1} accept="image/*">
              <Button icon={<UploadOutlined />}>Select Image</Button>
            </Upload>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
}
