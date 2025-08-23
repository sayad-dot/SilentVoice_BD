import React, { useEffect, useState } from 'react';
import { Table, Button, Tag, Space, Select, message } from 'antd';
import apiClient from '../../services/apiClient';

const { Option } = Select;

export default function UserManagement() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const response = await apiClient.get('/api/admin/users');
      setUsers(response.data);
    } catch (error) {
      console.error('Error fetching users:', error);
      message.error('Failed to fetch users');
    } finally {
      setLoading(false);
    }
  };

  const updateUserRole = async (userId, newRole) => {
    try {
      await apiClient.patch(`/api/admin/users/${userId}/role`, { role: newRole });
      message.success('User role updated successfully');
      fetchUsers(); // Refresh the list
    } catch (error) {
      console.error('Error updating user role:', error);
      message.error('Failed to update user role');
    }
  };

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 100,
      render: (id) => id.substring(0, 8) + '...',
    },
    {
      title: 'Email',
      dataIndex: 'email',
      key: 'email',
    },
    {
      title: 'Full Name',
      dataIndex: 'fullName',
      key: 'fullName',
    },
    {
      title: 'Provider',
      dataIndex: 'oauthProvider',
      key: 'oauthProvider',
      render: (provider) => (
        <Tag color={provider === 'google' ? 'blue' : 'green'}>
          {provider?.toUpperCase() || 'LOCAL'}
        </Tag>
      ),
    },
    {
      title: 'Roles',
      dataIndex: 'roles',
      key: 'roles',
      render: (roles) => (
        <>
          {roles?.map((role) => (
            <Tag key={role} color={role === 'ADMIN' ? 'red' : 'blue'}>
              {role}
            </Tag>
          ))}
        </>
      ),
    },
    {
      title: 'Actions',
      key: 'actions',
      render: (_, record) => (
        <Space>
          <Select
            defaultValue={record.roles?.[0] || 'USER'}
            onChange={(value) => updateUserRole(record.id, value)}
            style={{ width: 120 }}
          >
            <Option value="USER">User</Option>
            <Option value="ADMIN">Admin</Option>
          </Select>
        </Space>
      ),
    },
  ];

  return (
    <div className="admin-content">
      <div className="admin-content-header">
        <h1>User Management</h1>
        <p>Manage user accounts and roles</p>
      </div>
      
      <div className="admin-table">
        <Table
          columns={columns}
          dataSource={users}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 10 }}
        />
      </div>
    </div>
  );
}
