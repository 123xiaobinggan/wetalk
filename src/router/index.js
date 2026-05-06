import { createRouter, createWebHashHistory } from 'vue-router'

import EnterPage from '../pages/Enter/index.vue'

import HomePage from '../pages/Home/index.vue'

import Settings from '../pages/Settings/index.vue'
import AddFriends from '../pages/AddFriends/index.vue'
import AudioCall from '../pages/AudioCall/index.vue'

import RecentConversation from '../pages/Second/RecentConversation/index.vue'
import FriendsList from '../pages/Second/FriendsList/index.vue'
import FavoritesType from '../pages/Second/FavoritesType/index.vue'

import TalkWindow from '../pages/Third/TalkWindow/index.vue'
import FriendRequestDetail from '../pages/Third/FriendRequestDetail/index.vue'
import FriendDetail from '../pages/Third/FriendDetail/index.vue'
import GroupDetail from '../pages/Third/GroupDetail/index.vue'
import FavaritesContent from '../pages/Third/FavoritesContent/index.vue'

const routes = [
  {
    path: '/',
    component: EnterPage,
  },
  {
    path: '/home',
    component: HomePage,
    redirect: '/home/talk/recentConversation/talkwindow',
    children: [
      {
        path: 'talk',
        redirect: '/home/talk/recentConversation/talkwindow',
        children: [
          {
            path: 'recentConversation',
            component: RecentConversation,
            meta: { keepAlive: true },
            children: [
              {
                path: 'talkwindow',
                component: TalkWindow,
              },
            ],
          },
        ],
      },
      {
        path: 'friends',
        redirect: '/home/friends/friendsList/friendDetail',
        children: [
          {
            path: 'friendsList',
            name: 'FriendsListPage',
            component: FriendsList,
            meta: { keepAlive: true },
            children: [
              {
                path: 'friendRequestDetail',
                name: 'FriendRequestDetailPage',
                component: FriendRequestDetail,
              },
              {
                path: 'groupDetail',
                name: 'GroupDetailPage',
                component: GroupDetail,
              },
              {
                path: 'friendDetail',
                name: 'FriendDetailPage',
                component: FriendDetail,
              },
            ],
          },
        ],
      },
      {
        path: 'favorites',
        redirect: '/home/favorites/favoritesType/favoritesContent',
        children: [
          {
            path: 'favoritesType',
            component: FavoritesType,
            children: [
              {
                path: 'favoritesContent',
                component: FavaritesContent,
              },
            ],
          },
        ],
      },
    ],
  },
  {
    path: '/settings',
    component: Settings,
  },
  {
    path: '/addFriends',
    component: AddFriends,
  },
  {
    path: '/audioCall',
    component: AudioCall,
  },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

export default router
