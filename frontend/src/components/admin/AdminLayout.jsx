import React from 'react';
import { Layout, Menu } from 'antd';
import {
  UserOutlined,
  FileOutlined
} from '@ant-design/icons';
import './AdminLayout.css';

const { Sider, Content } = Layout;

export default function AdminLayout({ selectedKey, onSelect, children }) {
  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[selectedKey]}
          onClick={({ key }) => onSelect(key)}
        >
          <Menu.Item key="users" icon={<UserOutlined />}>
            User Management
          </Menu.Item>
          <Menu.Item key="content" icon={<FileOutlined />}>
            Content Management
          </Menu.Item>
        </Menu>
      </Sider>
      <Layout>
        <Content style={{ margin: '16px' }}>
          {children}
        </Content>
      </Layout>
    </Layout>
  );
}
