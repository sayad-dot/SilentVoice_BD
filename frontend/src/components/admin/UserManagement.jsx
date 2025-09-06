import React, { useEffect, useState } from 'react';
import { 
    Table, 
    Button, 
    Tag, 
    Space, 
    Select, 
    message, 
    Modal, 
    Card, 
    Statistic, 
    Row, 
    Col,
    Popconfirm,
    Avatar,
    Tooltip,
    Input
} from 'antd';
import { 
    UserOutlined, 
    DeleteOutlined, 
    EditOutlined,
    ExclamationCircleOutlined,
    SearchOutlined,
    GoogleOutlined,
    UserSwitchOutlined
} from '@ant-design/icons';
import apiClient from '../../services/apiClient';

const { Option } = Select;
const { Search } = Input;

export default function UserManagement() {
    const [users, setUsers] = useState([]);
    const [filteredUsers, setFilteredUsers] = useState([]);
    const [loading, setLoading] = useState(false);
    const [stats, setStats] = useState({});
    const [selectedUser, setSelectedUser] = useState(null);
    const [modalVisible, setModalVisible] = useState(false);
    const [searchText, setSearchText] = useState('');
    const [statusFilter, setStatusFilter] = useState('all');
    const [roleFilter, setRoleFilter] = useState('all');

    useEffect(() => {
        fetchUsers();
        fetchStats();
    }, []);

    useEffect(() => {
        filterUsers();
    }, [users, searchText, statusFilter, roleFilter]);

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

    const fetchStats = async () => {
        try {
            const response = await apiClient.get('/api/admin/users/stats');
            setStats(response.data);
        } catch (error) {
            console.error('Error fetching stats:', error);
        }
    };

    const filterUsers = () => {
        let filtered = [...users];

        // Search filter
        if (searchText) {
            filtered = filtered.filter(user => 
                user.email.toLowerCase().includes(searchText.toLowerCase()) ||
                user.fullName.toLowerCase().includes(searchText.toLowerCase())
            );
        }

        // Status filter
        if (statusFilter !== 'all') {
            filtered = filtered.filter(user => 
                (user.status || 'ACTIVE') === statusFilter
            );
        }

        // Role filter
        if (roleFilter !== 'all') {
            filtered = filtered.filter(user => 
                user.roleNames && user.roleNames.includes(roleFilter)
            );
        }

        setFilteredUsers(filtered);
    };

    const updateUserRole = async (userId, newRoles) => {
        try {
            // Handle both single role and multiple roles
            const roleValue = Array.isArray(newRoles) ? newRoles.join(',') : newRoles;
            await apiClient.patch(`/api/admin/users/${userId}/role`, { role: roleValue });
            message.success('User role updated successfully');
            fetchUsers();
        } catch (error) {
            console.error('Error updating user role:', error);
            message.error('Failed to update user role');
        }
    };

    const updateUserStatus = async (userId, newStatus) => {
        try {
            await apiClient.patch(`/api/admin/users/${userId}/status`, { status: newStatus });
            message.success('User status updated successfully');
            fetchUsers();
        } catch (error) {
            console.error('Error updating user status:', error);
            message.error('Failed to update user status');
        }
    };

    const deleteUser = async (userId) => {
        try {
            await apiClient.delete(`/api/admin/users/${userId}`);
            message.success('User deleted successfully');
            fetchUsers();
        } catch (error) {
            console.error('Error deleting user:', error);
            message.error('Failed to delete user');
        }
    };

    const getStatusColor = (status) => {
        switch (status) {
            case 'ACTIVE': return 'green';
            case 'SUSPENDED': return 'orange';
            case 'BANNED': return 'red';
            case 'DELETED': return 'gray';
            default: return 'default';
        }
    };

    const getRoleColor = (role) => {
        switch (role) {
            case 'ADMIN': return 'red';
            case 'MODERATOR': return 'blue';
            case 'USER': return 'default';
            default: return 'default';
        }
    };

    const columns = [
        {
            title: 'User',
            key: 'user',
            width: 250,
            render: (_, record) => (
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <Avatar 
                        src={record.profilePictureUrl} 
                        icon={<UserOutlined />}
                        size={32}
                    />
                    <div>
                        <div style={{ fontWeight: 500 }}>{record.fullName}</div>
                        <div style={{ fontSize: '12px', color: '#8c8c8c' }}>
                            {record.email}
                        </div>
                        <div style={{ fontSize: '11px', color: '#bfbfbf' }}>
                            ID: {record.id.substring(0, 8)}...
                        </div>
                    </div>
                </div>
            ),
        },
        {
            title: 'Provider',
            dataIndex: 'oauthProvider',
            key: 'oauthProvider',
            width: 100,
            render: (provider) => (
                <Tooltip title={`Authentication via ${provider || 'local'}`}>
                    {provider === 'google' ? (
                        <Tag icon={<GoogleOutlined />} color="blue">Google</Tag>
                    ) : (
                        <Tag color="green">Local</Tag>
                    )}
                </Tooltip>
            ),
        },
        {
            title: 'Status',
            dataIndex: 'status',
            key: 'status',
            width: 130,
            render: (status, record) => (
                <Select
                    value={status || 'ACTIVE'}
                    style={{ width: '100%' }}
                    onChange={(value) => updateUserStatus(record.id, value)}
                    size="small"
                >
                    <Option value="ACTIVE">
                        <Tag color="green" style={{ margin: 0 }}>Active</Tag>
                    </Option>
                    <Option value="SUSPENDED">
                        <Tag color="orange" style={{ margin: 0 }}>Suspended</Tag>
                    </Option>
                    <Option value="BANNED">
                        <Tag color="red" style={{ margin: 0 }}>Banned</Tag>
                    </Option>
                </Select>
            ),
        },
        {
            title: 'Roles',
            dataIndex: 'roleNames',
            key: 'roleNames',
            width: 180,
            render: (roleNames, record) => (
                <Select
                    mode="multiple"
                    style={{ width: '100%' }}
                    value={roleNames || ['USER']}
                    onChange={(values) => updateUserRole(record.id, values)}
                    size="small"
                    placeholder="Select roles"
                >
                    <Option value="USER">
                        <Tag color="default" style={{ margin: 0 }}>User</Tag>
                    </Option>
                    <Option value="MODERATOR">
                        <Tag color="blue" style={{ margin: 0 }}>Moderator</Tag>
                    </Option>
                    <Option value="ADMIN">
                        <Tag color="red" style={{ margin: 0 }}>Admin</Tag>
                    </Option>
                </Select>
            ),
        },
        {
            title: 'Login Stats',
            key: 'loginStats',
            width: 120,
            render: (_, record) => (
                <div style={{ fontSize: '12px' }}>
                    <div><strong>{record.loginCount || 0}</strong> logins</div>
                    <div style={{ color: '#8c8c8c' }}>
                        {record.lastLoginAt ? 
                            new Date(record.lastLoginAt).toLocaleDateString() : 
                            'Never'
                        }
                    </div>
                </div>
            ),
        },
        {
            title: 'Actions',
            key: 'actions',
            width: 120,
            render: (_, record) => (
                <Space>
                    <Tooltip title="View Details">
                        <Button
                            type="primary"
                            icon={<EditOutlined />}
                            size="small"
                            onClick={() => {
                                setSelectedUser(record);
                                setModalVisible(true);
                            }}
                        />
                    </Tooltip>
                    <Popconfirm
                        title="Delete User"
                        description="Are you sure you want to delete this user? This action cannot be undone."
                        onConfirm={() => deleteUser(record.id)}
                        okText="Yes"
                        cancelText="No"
                        icon={<ExclamationCircleOutlined style={{ color: 'red' }} />}
                    >
                        <Tooltip title="Delete User">
                            <Button
                                danger
                                icon={<DeleteOutlined />}
                                size="small"
                            />
                        </Tooltip>
                    </Popconfirm>
                </Space>
            ),
        },
    ];

    return (
        <div className="admin-content-body">
            {/* Statistics Cards */}
            <Row gutter={16} style={{ marginBottom: 24 }}>
                <Col span={6}>
                    <Card>
                        <Statistic
                            title="Total Users"
                            value={stats.totalUsers || users.length}
                            prefix={<UserOutlined />}
                        />
                    </Card>
                </Col>
                <Col span={6}>
                    <Card>
                        <Statistic
                            title="Active Users"
                            value={stats.activeUsers || users.filter(u => (u.status || 'ACTIVE') === 'ACTIVE').length}
                            valueStyle={{ color: '#3f8600' }}
                        />
                    </Card>
                </Col>
                <Col span={6}>
                    <Card>
                        <Statistic
                            title="New This Month"
                            value={stats.newUsersThisMonth || 0}
                            valueStyle={{ color: '#1890ff' }}
                        />
                    </Card>
                </Col>
                <Col span={6}>
                    <Card>
                        <Statistic
                            title="Suspended/Banned"
                            value={stats.suspendedUsers || users.filter(u => ['SUSPENDED', 'BANNED'].includes(u.status)).length}
                            valueStyle={{ color: '#cf1322' }}
                        />
                    </Card>
                </Col>
            </Row>

            {/* Filters */}
            <Card style={{ marginBottom: 16 }}>
                <Row gutter={16} align="middle">
                    <Col span={8}>
                        <Search
                            placeholder="Search by email or name"
                            allowClear
                            onChange={(e) => setSearchText(e.target.value)}
                            style={{ width: '100%' }}
                        />
                    </Col>
                    <Col span={4}>
                        <Select
                            placeholder="Filter by status"
                            value={statusFilter}
                            onChange={setStatusFilter}
                            style={{ width: '100%' }}
                        >
                            <Option value="all">All Status</Option>
                            <Option value="ACTIVE">Active</Option>
                            <Option value="SUSPENDED">Suspended</Option>
                            <Option value="BANNED">Banned</Option>
                        </Select>
                    </Col>
                    <Col span={4}>
                        <Select
                            placeholder="Filter by role"
                            value={roleFilter}
                            onChange={setRoleFilter}
                            style={{ width: '100%' }}
                        >
                            <Option value="all">All Roles</Option>
                            <Option value="USER">User</Option>
                            <Option value="MODERATOR">Moderator</Option>
                            <Option value="ADMIN">Admin</Option>
                        </Select>
                    </Col>
                    <Col span={8}>
                        <div style={{ textAlign: 'right', color: '#8c8c8c' }}>
                            Showing {filteredUsers.length} of {users.length} users
                        </div>
                    </Col>
                </Row>
            </Card>

            {/* Users Table */}
            <Card title="User Management" className="admin-table">
                <Table
                    columns={columns}
                    dataSource={filteredUsers}
                    rowKey="id"
                    loading={loading}
                    pagination={{
                        pageSize: 20,
                        showSizeChanger: true,
                        showQuickJumper: true,
                        showTotal: (total, range) => 
                            `${range[0]}-${range[1]} of ${total} users`,
                    }}
                    scroll={{ x: 1000 }}
                />
            </Card>

            {/* User Detail Modal */}
            <Modal
                title={
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                        <Avatar 
                            src={selectedUser?.profilePictureUrl} 
                            icon={<UserOutlined />}
                        />
                        <span>User Details - {selectedUser?.fullName}</span>
                    </div>
                }
                visible={modalVisible}
                onCancel={() => setModalVisible(false)}
                footer={null}
                width={700}
            >
                {selectedUser && (
                    <div>
                        <Row gutter={16}>
                            <Col span={12}>
                                <div style={{ marginBottom: 16 }}>
                                    <h4>Basic Information</h4>
                                    <p><strong>Email:</strong> {selectedUser.email}</p>
                                    <p><strong>Full Name:</strong> {selectedUser.fullName}</p>
                                    <p><strong>User ID:</strong> {selectedUser.id}</p>
                                    <p>
                                        <strong>Provider:</strong> 
                                        {selectedUser.oauthProvider === 'google' ? (
                                            <Tag icon={<GoogleOutlined />} color="blue" style={{ marginLeft: 8 }}>
                                                Google
                                            </Tag>
                                        ) : (
                                            <Tag color="green" style={{ marginLeft: 8 }}>Local</Tag>
                                        )}
                                    </p>
                                    <p>
                                        <strong>Status:</strong> 
                                        <Tag color={getStatusColor(selectedUser.status)} style={{ marginLeft: 8 }}>
                                            {selectedUser.status || 'ACTIVE'}
                                        </Tag>
                                    </p>
                                </div>
                            </Col>
                            <Col span={12}>
                                <div style={{ marginBottom: 16 }}>
                                    <h4>Account Activity</h4>
                                    <p><strong>Created:</strong> {selectedUser.createdAt ? 
                                        new Date(selectedUser.createdAt).toLocaleString() : 'Unknown'}</p>
                                    <p><strong>Last Updated:</strong> {selectedUser.updatedAt ? 
                                        new Date(selectedUser.updatedAt).toLocaleString() : 'Unknown'}</p>
                                    <p><strong>Last Login:</strong> {selectedUser.lastLoginAt ? 
                                        new Date(selectedUser.lastLoginAt).toLocaleString() : 'Never'}</p>
                                    <p><strong>Login Count:</strong> {selectedUser.loginCount || 0}</p>
                                    <p><strong>Email Verified:</strong> {selectedUser.emailVerified ? 'Yes' : 'No'}</p>
                                </div>
                            </Col>
                        </Row>
                        
                        <div style={{ marginTop: 16 }}>
                            <h4>Roles & Permissions</h4>
                            <div style={{ marginTop: 8 }}>
                                {(selectedUser.roleNames || ['USER']).map(role => (
                                    <Tag key={role} color={getRoleColor(role)} style={{ margin: '4px' }}>
                                        {role}
                                    </Tag>
                                ))}
                            </div>
                        </div>

                        {selectedUser.profilePictureUrl && (
                            <div style={{ marginTop: 16 }}>
                                <h4>Profile Picture</h4>
                                <Avatar 
                                    src={selectedUser.profilePictureUrl} 
                                    size={64}
                                />
                            </div>
                        )}
                    </div>
                )}
            </Modal>
        </div>
    );
}
